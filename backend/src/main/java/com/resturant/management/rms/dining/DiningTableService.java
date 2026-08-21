package com.resturant.management.rms.dining;

import com.resturant.management.rms.common.ConflictHelper;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.dining.dto.DiningDtos.TableRequest;
import com.resturant.management.rms.dining.dto.DiningDtos.TableResponse;
import com.resturant.management.rms.dining.dto.DiningDtos.TableSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository tableRepository;

    @Transactional(readOnly = true)
    public Page<TableResponse> search(String query, TableZone zone, Pageable pageable) {
        return tableRepository.search(blankToNull(query), zone, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TableResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public TableSummary summary() {
        return new TableSummary(
                tableRepository.countByStatus(TableStatus.FREE),
                tableRepository.countByStatus(TableStatus.OCCUPIED),
                tableRepository.countByStatus(TableStatus.RESERVED),
                tableRepository.count());
    }

    @Transactional
    public TableResponse create(TableRequest request) {
        if (tableRepository.existsByName(request.name())) {
            throw ConflictHelper.duplicate("entity.table", "field.name", request.name());
        }
        DiningTable table = new DiningTable();
        apply(table, request);
        return toResponse(tableRepository.save(table));
    }

    @Transactional
    public TableResponse update(Long id, TableRequest request) {
        DiningTable table = find(id);

        // Only a clash with a *different* row is a conflict — renaming a table
        // to its own current name must stay allowed.
        tableRepository.findByName(request.name())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw ConflictHelper.duplicate("entity.table", "field.name", request.name());
                });

        apply(table, request);
        return toResponse(tableRepository.save(table));
    }

    @Transactional
    public TableResponse changeStatus(Long id, TableStatus status) {
        DiningTable table = find(id);
        table.setStatus(status);
        return toResponse(tableRepository.save(table));
    }

    @Transactional
    public void delete(Long id) {
        DiningTable table = find(id);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new com.resturant.management.rms.common.exception.ConflictException(
                    "error.table.occupied", table.getName());
        }
        tableRepository.delete(table);
    }

    /* ---- Helpers --------------------------------------------------------- */

    private DiningTable find(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.table", id));
    }

    private void apply(DiningTable table, TableRequest r) {
        table.setName(r.name());
        table.setSeats(r.seats());
        table.setZone(r.zone() != null ? r.zone() : TableZone.INDOOR);
        table.setStatus(r.status() != null ? r.status() : TableStatus.FREE);
    }

    private TableResponse toResponse(DiningTable t) {
        return new TableResponse(t.getId(), t.getName(), t.getSeats(), t.getZone(), t.getStatus());
    }
}
