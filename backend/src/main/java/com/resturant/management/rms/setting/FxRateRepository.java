package com.resturant.management.rms.setting;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    List<FxRate> findByCurrencyOrderByValidFromDescIdDesc(String currency, Limit limit);

    Optional<FxRate> findByCurrencyAndValidFrom(String currency, LocalDate validFrom);

    /** The rate in force on a given day. */
    Optional<FxRate> findFirstByCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
            String currency, LocalDate on);
}
