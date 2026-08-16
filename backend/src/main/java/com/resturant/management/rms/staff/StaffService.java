package com.resturant.management.rms.staff;

import com.resturant.management.rms.common.ConflictHelper;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.staff.dto.StaffDtos.StaffRequest;
import com.resturant.management.rms.staff.dto.StaffDtos.StaffResponse;
import com.resturant.management.rms.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public Page<StaffResponse> search(String query, Role role, Pageable pageable) {
        return staffRepository.search(blankToNull(query), role, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        if (staffRepository.existsByStaffCode(request.staffCode())) {
            throw ConflictHelper.duplicate("Staff", "code", request.staffCode());
        }
        Staff staff = new Staff();
        apply(staff, request);
        return toResponse(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = find(id);

        staffRepository.findByStaffCode(request.staffCode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw ConflictHelper.duplicate("Staff", "code", request.staffCode());
                });

        apply(staff, request);
        return toResponse(staffRepository.save(staff));
    }

    @Transactional
    public void delete(Long id) {
        staffRepository.delete(find(id));
    }

    /* ---- Helpers --------------------------------------------------------- */

    private Staff find(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Staff", id));
    }

    private void apply(Staff staff, StaffRequest r) {
        staff.setStaffCode(r.staffCode());
        staff.setStaffName(r.staffName());
        staff.setGender(r.gender());
        staff.setDateOfBirth(r.dateOfBirth());
        staff.setPhone(r.phone());
        staff.setEmail(r.email());
        staff.setRole(r.role());
        staff.setShift(r.shift());
        staff.setSalary(r.salary());
        staff.setHireDate(r.hireDate());
        staff.setAddress(r.address());
        staff.setStatus(r.status() != null ? r.status() : StaffStatus.ACTIVE);
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(
                s.getId(), s.getStaffCode(), s.getStaffName(), s.getGender(),
                s.getDateOfBirth(), s.getPhone(), s.getEmail(), s.getRole(),
                s.getShift(), s.getSalary(), s.getHireDate(), s.getAddress(),
                s.getStatus(), s.getUser() != null ? s.getUser().getId() : null);
    }
}
