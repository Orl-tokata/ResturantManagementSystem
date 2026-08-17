package com.resturant.management.rms;

import com.resturant.management.rms.catalog.CategoryRepository;
import com.resturant.management.rms.catalog.ProductRepository;
import com.resturant.management.rms.dining.DiningTableRepository;
import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.setting.AppSettingRepository;
import com.resturant.management.rms.staff.StaffRepository;
import com.resturant.management.rms.stock.StockItemRepository;
import com.resturant.management.rms.supplier.SupplierRepository;
import com.resturant.management.rms.user.Role;
import com.resturant.management.rms.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 2 verification.
 *
 * <p>Runs on the {@code dev} profile: H2 in PostgreSQL mode, Flyway on,
 * {@code ddl-auto=validate}. Starting the context at all proves the migrations
 * apply and the entity mappings match the schema; the assertions below prove
 * the seed data landed and the repository queries actually execute.
 */
@SpringBootTest
class RmsApplicationTests {

	@Autowired CategoryRepository categories;
	@Autowired ProductRepository products;
	@Autowired DiningTableRepository tables;
	@Autowired StaffRepository staff;
	@Autowired SupplierRepository suppliers;
	@Autowired StockItemRepository stockItems;
	@Autowired AppSettingRepository settings;
	@Autowired UserRepository users;

	@Test
	@DisplayName("context loads: migrations apply and entities validate against the schema")
	void contextLoads() {
	}

	@Test
	@DisplayName("V3 seed data is present")
	void seedDataLoaded() {
		assertThat(categories.count()).isEqualTo(8);
		assertThat(products.count()).isEqualTo(16);
		assertThat(tables.count()).isEqualTo(12);
		assertThat(staff.count()).isEqualTo(7);
		assertThat(suppliers.count()).isEqualTo(5);
		assertThat(stockItems.count()).isEqualTo(10);
		assertThat(settings.count()).isEqualTo(15);
	}

	@Test
	@DisplayName("DataInitializer creates the admin and cashier accounts, hashed")
	void initialAccountsCreated() {
		var admin = users.findByUserId("admin").orElseThrow();
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.isEnabled()).isTrue();
		assertThat(admin.getUserPwd()).startsWith("$2");        // BCrypt, not plaintext
		assertThat(admin.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");

		assertThat(users.findByUserId("cashier").orElseThrow().getRole()).isEqualTo(Role.CASHIER);
	}

	@Test
	@DisplayName("custom @Query methods execute against the database")
	void customQueriesRun() {
		assertThat(products.search("បាយ", null, PageRequest.of(0, 10))).isNotEmpty();
		assertThat(products.search(null, 7L, PageRequest.of(0, 10))).isNotEmpty();   // drinks
		assertThat(categories.search(null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(8);
		assertThat(suppliers.search("Angkor", PageRequest.of(0, 10))).hasSize(1);
		assertThat(staff.search(null, Role.CASHIER, PageRequest.of(0, 10))).hasSize(2);
	}

	@Test
	@DisplayName("low-stock queries reflect the seeded quantities")
	void lowStockDetection() {
		// Seeded below their minimum: beef, prawn, beer, water, cooking oil.
		assertThat(stockItems.findLowStock()).hasSize(5);
		assertThat(stockItems.countOutOfStock()).isEqualTo(1);      // ប្រេងឆា at 0
		assertThat(stockItems.totalStockValue()).isPositive();
	}

	@Test
	@DisplayName("table status counts match the prototype's table picker")
	void tableStatusCounts() {
		assertThat(tables.countByStatus(TableStatus.FREE)).isEqualTo(6);
		assertThat(tables.countByStatus(TableStatus.OCCUPIED)).isEqualTo(5);
		assertThat(tables.countByStatus(TableStatus.RESERVED)).isEqualTo(1);
	}
}
