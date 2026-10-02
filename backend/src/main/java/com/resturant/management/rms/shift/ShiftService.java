package com.resturant.management.rms.shift;

import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.shift.dto.ShiftDtos.*;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Opening, closing and counting a till.
 *
 * <p>The rule worth stating: a cashier has at most one open shift, and the
 * database is what says so. The check here exists to give a person a sentence
 * they can act on; the unique constraint is what holds when two tills send the
 * request at the same moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftService {

    private static final int MONEY_SCALE = 2;

    private final ShiftRepository shifts;
    private final CashMovementRepository movements;
    private final UserRepository users;

    /* ===================================================================== */
    /* Opening and closing                                                   */
    /* ===================================================================== */

    @Transactional
    public ShiftResponse open(OpenShiftRequest request, String username) {
        UserInfm user = user(username);

        if (shifts.findByOpenUserRef(user.getId()).isPresent()) {
            throw new BadRequestException("error.shift.alreadyOpen");
        }

        CashShift shift = CashShift.open(user, scale(request.openingFloat()));
        shift.setNote(Strings.blankToNull(request.note()));

        try {
            shifts.save(shift);
            shifts.flush();
        } catch (DataIntegrityViolationException e) {
            // The check above lost a race with another till. The constraint is
            // the thing that actually enforces this, so arriving here is the
            // rule working rather than a failure.
            log.info("Two shift opens raced for '{}'; the database refused the second", username);
            throw new BadRequestException("error.shift.alreadyOpen");
        }

        log.info("Shift {} opened by {} with a float of {}",
                shift.getId(), username, shift.getOpeningFloat());
        return toResponse(shift);
    }

    /**
     * Closes a shift against a counted drawer.
     *
     * <p>The variance is recorded whatever it is. A till that refuses to close
     * on a discrepancy teaches its cashiers to declare the expected figure,
     * which turns the one number worth having into a formality.
     */
    @Transactional
    public ShiftResponse close(Long id, CloseShiftRequest request, String username) {
        CashShift shift = find(id);

        if (!shift.isOpen()) {
            throw new BadRequestException("error.shift.notOpen");
        }
        if (!shift.getUser().getUserId().equals(username) && !isManager()) {
            throw new BadRequestException("error.shift.notYours");
        }

        BigDecimal declared = scale(request.declaredCash());
        BigDecimal expected = expectedCash(shift);
        BigDecimal variance = declared.subtract(expected);
        String note = Strings.blankToNull(request.note());

        // A discrepancy nobody explained is the one thing worth insisting on:
        // it is the difference between a count and a question.
        if (variance.signum() != 0 && note == null) {
            throw new BadRequestException(
                    variance.signum() < 0 ? "error.shift.shortNeedsNote" : "error.shift.overNeedsNote",
                    variance.abs());
        }

        shift.close(declared, expected, note);
        shifts.save(shift);

        log.info("Shift {} closed by {} — expected {}, declared {}, variance {}",
                shift.getId(), username, expected, declared, variance);
        return toResponse(shift);
    }

    /* ===================================================================== */
    /* Movements                                                             */
    /* ===================================================================== */

    /**
     * Records money in or out by hand.
     *
     * <p>SALE and REFUND are refused here. They follow from bills, and a second
     * way to write them is a second way for the drawer to disagree with the
     * sales it came from.
     */
    @Transactional
    public CashMovementResponse record(Long shiftId, CashMovementRequest request, String username) {
        CashShift shift = find(shiftId);
        if (!shift.isOpen()) {
            throw new BadRequestException("error.shift.notOpen");
        }
        if (!request.type().isEnteredByHand()) {
            throw new BadRequestException("error.shift.typeNotManual", request.type().name());
        }

        CashMovement movement = movements.save(CashMovement.builder()
                .shift(shift)
                .type(request.type())
                .amount(scale(request.amount()))
                .reason(Strings.blankToNull(request.reason()))
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build());

        return toResponse(movement);
    }

    /**
     * Writes a sale into the drawer.
     *
     * <p>Called by the settlement path for cash only: a card does not put notes
     * in a till. The amount is what the payment covered, not what was handed
     * over — the change went straight back out again.
     *
     * <p>Silent when there is no open shift. The gate refuses a sale without
     * one, so reaching this with none means an order settled by some path the
     * gate does not cover, and losing the sale would be worse than a drawer
     * that cannot account for it.
     */
    @Transactional
    public void recordSale(String username, BigDecimal amount, Long orderId, String invoiceNo) {
        Optional<CashShift> open = openShiftOf(username);
        if (open.isEmpty()) {
            log.warn("Cash sale on {} with no open shift for '{}' — not in any drawer",
                    invoiceNo, username);
            return;
        }

        movements.save(CashMovement.builder()
                .shift(open.get())
                .type(CashMovementType.SALE)
                .amount(scale(amount))
                .reason(invoiceNo)
                .refType(CashMovement.REF_ORDER)
                .refId(orderId)
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /* ===================================================================== */
    /* The gate                                                              */
    /* ===================================================================== */

    /**
     * What a till session is, as far as the rest of the application is
     * concerned: present or absent.
     */
    @Transactional(readOnly = true)
    public Optional<CashShift> openShiftOf(String username) {
        return users.findByUserId(username)
                .flatMap(u -> shifts.findByOpenUserRef(u.getId()));
    }

    /**
     * Refuses an action that moves money when nobody has opened the drawer.
     *
     * <p>This is the gate, and it lives here rather than only in the screen,
     * because a rule enforced by a redirect is a rule that an API call does not
     * have to follow. Without it every cash report is a guess about which
     * sales belong to which session.
     */
    @Transactional(readOnly = true)
    public CashShift requireOpenShift(String username) {
        return openShiftOf(username)
                .orElseThrow(() -> new BadRequestException("error.shift.required"));
    }

    /* ===================================================================== */
    /* Reads                                                                 */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public Optional<ShiftResponse> current(String username) {
        return openShiftOf(username).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShiftDetail detail(Long id) {
        CashShift shift = find(id);
        return new ShiftDetail(
                toResponse(shift),
                movements.findByShiftIdOrderByIdAsc(id).stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public Page<ShiftResponse> history(Pageable pageable) {
        return shifts.findAllByOrderByOpenedAtDesc(pageable).map(this::toResponse);
    }

    /* ===================================================================== */
    /* Arithmetic                                                            */
    /* ===================================================================== */

    /**
     * The float plus everything that moved.
     *
     * <p>Recomputed from the rows every time while the shift is open. Once it
     * is closed the stored figure stands, because that is what the count was
     * measured against and a movement corrected afterwards must not rewrite a
     * variance somebody already signed off.
     */
    private BigDecimal expectedCash(CashShift shift) {
        if (!shift.isOpen() && shift.getExpectedCash() != null) {
            return shift.getExpectedCash();
        }
        return scale(shift.getOpeningFloat().add(shifts.netMovement(shift.getId())));
    }

    private CashShift find(Long id) {
        return shifts.findById(id).orElseThrow(() -> NotFoundException.of("entity.shift", id));
    }

    private UserInfm user(String username) {
        return users.findByUserId(username)
                .orElseThrow(() -> NotFoundException.of("entity.user", username));
    }

    private boolean isManager() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /* ===================================================================== */
    /* Mapping                                                               */
    /* ===================================================================== */

    private ShiftResponse toResponse(CashShift shift) {
        List<MovementTotal> totals = movements.totalsByType(shift.getId()).stream()
                .map(row -> new MovementTotal(
                        (CashMovementType) row[0],
                        scale((BigDecimal) row[1]),
                        ((Number) row[2]).longValue()))
                .toList();

        MovementTotal sales = totals.stream()
                .filter(t -> t.type() == CashMovementType.SALE)
                .findFirst()
                .orElse(new MovementTotal(CashMovementType.SALE, BigDecimal.ZERO, 0));

        return new ShiftResponse(
                shift.getId(),
                shift.getUser().getId(),
                shift.getUser().getUserNm(),
                shift.getStatus(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getOpeningFloat(),
                expectedCash(shift),
                shift.getDeclaredCash(),
                shift.getVariance(),
                shift.getNote(),
                totals,
                sales.total(),
                sales.count());
    }

    private CashMovementResponse toResponse(CashMovement m) {
        return new CashMovementResponse(
                m.getId(), m.getType(), m.getAmount(), m.getType().isIncrease(),
                m.getReason(), m.getRefType(), m.getRefId(),
                m.getCreatedBy(), m.getCreatedAt());
    }
}
