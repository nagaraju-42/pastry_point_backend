//package com.bakeryq.config;
//
//import com.bakeryq.entity.*;
//import com.bakeryq.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.modelmapper.ModelMapper;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class AppConfig {
//
//    @Bean
//    public ModelMapper modelMapper() {
//        return new ModelMapper();
//    }
//
//    // ─── Seed demo data when running locally ─────────────────────────────────
//    @Bean
//    @Profile("dev")
//    public CommandLineRunner seedData(
//            UserRepository userRepo,
//            CategoryRepository categoryRepo,
//            MenuItemRepository menuItemRepo,
//            PasswordEncoder passwordEncoder) {
//
//        return args -> {
//            if (userRepo.count() > 0) {
//                log.info("Database already seeded — skipping.");
//                return;
//            }
//
//            log.info("Seeding demo data...");
//
//            // ── Users ──────────────────────────────────────────────────────
//            User admin = userRepo.save(User.builder()
//                    .name("Shop Owner")
//                    .email("admin@bakeryq.com")
//                    .phone("9000000001")
//                    .password(passwordEncoder.encode("admin123"))
//                    .role(User.Role.ADMIN)
//                    .build());
//
//            User kitchen = userRepo.save(User.builder()
//                    .name("Kitchen Staff")
//                    .email("kitchen@bakeryq.com")
//                    .phone("9000000002")
//                    .password(passwordEncoder.encode("kitchen123"))
//                    .role(User.Role.KITCHEN_STAFF)
//                    .build());
//
//            User student = userRepo.save(User.builder()
//                    .name("Arjun Sharma")
//                    .email("student@test.com")
//                    .phone("9876543210")
//                    .password(passwordEncoder.encode("test123"))
//                    .role(User.Role.STUDENT)
//                    .loyaltyPoints(150)
//                    .build());
//
//            // ── Categories ────────────────────────────────────────────────
//            Category breads    = categoryRepo.save(Category.builder().name("Breads").displayOrder(1).build());
//            Category pastries  = categoryRepo.save(Category.builder().name("Pastries").displayOrder(2).build());
//            Category cakes     = categoryRepo.save(Category.builder().name("Cakes").displayOrder(3).build());
//            Category beverages = categoryRepo.save(Category.builder().name("Beverages").displayOrder(4).build());
//            Category snacks    = categoryRepo.save(Category.builder().name("Snacks").displayOrder(5).build());
//
//            // ── Menu Items ────────────────────────────────────────────────
//            List<MenuItem> items = List.of(
//                // Breads
//                MenuItem.builder().name("Butter Croissant").price(new BigDecimal("60")).category(breads)
//                        .description("Flaky, buttery French croissant baked fresh every morning")
//                        .preparationTimeMinutes(5).featured(true).stockQuantity(50).build(),
//                MenuItem.builder().name("Whole Wheat Bread Loaf").price(new BigDecimal("80")).category(breads)
//                        .description("Soft whole wheat loaf, great for sandwiches")
//                        .preparationTimeMinutes(3).stockQuantity(30).build(),
//                MenuItem.builder().name("Garlic Bread (6 pcs)").price(new BigDecimal("70")).category(breads)
//                        .description("Toasted baguette slices with garlic butter and herbs")
//                        .preparationTimeMinutes(8).featured(true).stockQuantity(40).build(),
//                MenuItem.builder().name("Multigrain Bun").price(new BigDecimal("25")).category(breads)
//                        .description("Nutritious multigrain bun, perfect with soup").preparationTimeMinutes(3).stockQuantity(60).build(),
//
//                // Pastries
//                MenuItem.builder().name("Chocolate Éclair").price(new BigDecimal("55")).category(pastries)
//                        .description("Classic French éclair filled with cream and topped with chocolate").preparationTimeMinutes(2).featured(true).stockQuantity(35).build(),
//                MenuItem.builder().name("Apple Turnover").price(new BigDecimal("65")).category(pastries)
//                        .description("Flaky pastry filled with cinnamon spiced apple filling").preparationTimeMinutes(2).stockQuantity(25).build(),
//                MenuItem.builder().name("Cheese Danish").price(new BigDecimal("70")).category(pastries)
//                        .description("Sweet pastry with rich cream cheese filling").preparationTimeMinutes(2).stockQuantity(30).build(),
//                MenuItem.builder().name("Almond Croissant").price(new BigDecimal("75")).category(pastries)
//                        .description("Buttery croissant filled with almond cream and topped with sliced almonds").preparationTimeMinutes(5).stockQuantity(20).build(),
//
//                // Cakes
//                MenuItem.builder().name("Chocolate Truffle Pastry").price(new BigDecimal("90")).category(cakes)
//                        .description("Rich dark chocolate truffle cake slice").preparationTimeMinutes(2).featured(true).stockQuantity(20).build(),
//                MenuItem.builder().name("Blueberry Cheesecake Slice").price(new BigDecimal("110")).category(cakes)
//                        .description("Creamy New York style cheesecake with blueberry compote").preparationTimeMinutes(2).stockQuantity(15).build(),
//                MenuItem.builder().name("Red Velvet Slice").price(new BigDecimal("95")).category(cakes)
//                        .description("Moist red velvet cake with cream cheese frosting").preparationTimeMinutes(2).stockQuantity(18).build(),
//                MenuItem.builder().name("Tiramisu Cup").price(new BigDecimal("120")).category(cakes)
//                        .description("Classic Italian dessert with mascarpone and espresso").preparationTimeMinutes(2).stockQuantity(12).build(),
//
//                // Beverages
//                MenuItem.builder().name("Café Latte").price(new BigDecimal("80")).category(beverages)
//                        .isVeg(true).description("Smooth espresso with steamed milk").preparationTimeMinutes(5).featured(true).stockQuantity(100).build(),
//                MenuItem.builder().name("Hot Chocolate").price(new BigDecimal("75")).category(beverages)
//                        .isVeg(true).description("Rich, creamy Belgian hot chocolate").preparationTimeMinutes(5).stockQuantity(100).build(),
//                MenuItem.builder().name("Cold Coffee").price(new BigDecimal("90")).category(beverages)
//                        .isVeg(true).description("Chilled coffee blended with milk and ice cream").preparationTimeMinutes(5).stockQuantity(100).build(),
//                MenuItem.builder().name("Fresh Orange Juice").price(new BigDecimal("70")).category(beverages)
//                        .isVeg(true).description("Freshly squeezed orange juice").preparationTimeMinutes(5).stockQuantity(50).build(),
//
//                // Snacks
//                MenuItem.builder().name("Veg Sandwich").price(new BigDecimal("55")).category(snacks)
//                        .description("Toasted sandwich with fresh veggies and cheese").preparationTimeMinutes(8).featured(true).stockQuantity(40).build(),
//                MenuItem.builder().name("Samosa (2 pcs)").price(new BigDecimal("30")).category(snacks)
//                        .description("Crispy fried samosas with mint chutney").preparationTimeMinutes(5).stockQuantity(60).build(),
//                MenuItem.builder().name("Veg Puff").price(new BigDecimal("25")).category(snacks)
//                        .description("Flaky pastry puff stuffed with spiced vegetables").preparationTimeMinutes(3).stockQuantity(50).build(),
//                MenuItem.builder().name("Cookie Box (4 pcs)").price(new BigDecimal("80")).category(snacks)
//                        .description("Assorted freshly baked cookies — choco chip, oatmeal, and more").preparationTimeMinutes(2).stockQuantity(30).build()
//            );
//
//            menuItemRepo.saveAll(items);
//
//            log.info("✅ Seeded {} menu items across {} categories.", items.size(), 5);
//            log.info("✅ Admin login  → admin@bakeryq.com / admin123");
//            log.info("✅ Student login → student@test.com / test123");
//        };
//    }
//}

package com.bakeryq.config;

import com.bakeryq.entity.*;
import com.bakeryq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    // ─── Seed demo data when running locally ─────────────────────────────────
    @Bean
    @Profile("dev")
    public CommandLineRunner seedData(
            UserRepository userRepo,
            CategoryRepository categoryRepo,
            MenuItemRepository menuItemRepo,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepo.count() > 0) {
                log.info("Database already seeded — skipping.");
                return;
            }

            log.info("Seeding demo data...");

            // ── Users ──────────────────────────────────────────────────────
            User admin = userRepo.save(User.builder()
                    .name("Shop Owner")
                    .email("admin@bakeryq.com")
                    .phone("9000000001")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build());

            User kitchen = userRepo.save(User.builder()
                    .name("Kitchen Staff")
                    .email("kitchen@bakeryq.com")
                    .phone("9000000002")
                    .password(passwordEncoder.encode("kitchen123"))
                    .role(User.Role.KITCHEN_STAFF)
                    .build());

            User student = userRepo.save(User.builder()
                    .name("Arjun Sharma")
                    .email("student@test.com")
                    .phone("9876543210")
                    .password(passwordEncoder.encode("test123"))
                    .role(User.Role.STUDENT)
                    .loyaltyPoints(150)
                    .build());

            // ── Categories ────────────────────────────────────────────────
            Category breads    = categoryRepo.save(Category.builder().name("Breads").displayOrder(1).build());
            Category pastries  = categoryRepo.save(Category.builder().name("Pastries").displayOrder(2).build());
            Category cakes     = categoryRepo.save(Category.builder().name("Cakes").displayOrder(3).build());
            Category beverages = categoryRepo.save(Category.builder().name("Beverages").displayOrder(4).build());
            Category snacks    = categoryRepo.save(Category.builder().name("Snacks").displayOrder(5).build());

            // ── Menu Items ────────────────────────────────────────────────
            List<MenuItem> items = List.of(
                // Breads
                MenuItem.builder().name("Butter Croissant").price(new BigDecimal("60")).category(breads)
                        .description("Flaky, buttery French croissant baked fresh every morning")
                        .preparationTimeMinutes(5).featured(true).stockQuantity(50).build(),
                MenuItem.builder().name("Whole Wheat Bread Loaf").price(new BigDecimal("80")).category(breads)
                        .description("Soft whole wheat loaf, great for sandwiches")
                        .preparationTimeMinutes(3).stockQuantity(30).build(),
                MenuItem.builder().name("Garlic Bread (6 pcs)").price(new BigDecimal("70")).category(breads)
                        .description("Toasted baguette slices with garlic butter and herbs")
                        .preparationTimeMinutes(8).featured(true).stockQuantity(40).build(),
                MenuItem.builder().name("Multigrain Bun").price(new BigDecimal("25")).category(breads)
                        .description("Nutritious multigrain bun, perfect with soup").preparationTimeMinutes(3).stockQuantity(60).build(),

                // Pastries
                MenuItem.builder().name("Chocolate Éclair").price(new BigDecimal("55")).category(pastries)
                        .description("Classic French éclair filled with cream and topped with chocolate").preparationTimeMinutes(2).featured(true).stockQuantity(35).build(),
                MenuItem.builder().name("Apple Turnover").price(new BigDecimal("65")).category(pastries)
                        .description("Flaky pastry filled with cinnamon spiced apple filling").preparationTimeMinutes(2).stockQuantity(25).build(),
                MenuItem.builder().name("Cheese Danish").price(new BigDecimal("70")).category(pastries)
                        .description("Sweet pastry with rich cream cheese filling").preparationTimeMinutes(2).stockQuantity(30).build(),
                MenuItem.builder().name("Almond Croissant").price(new BigDecimal("75")).category(pastries)
                        .description("Buttery croissant filled with almond cream and topped with sliced almonds").preparationTimeMinutes(5).stockQuantity(20).build(),

                // Cakes
                MenuItem.builder().name("Chocolate Truffle Pastry").price(new BigDecimal("90")).category(cakes)
                        .description("Rich dark chocolate truffle cake slice").preparationTimeMinutes(2).featured(true).stockQuantity(20).build(),
                MenuItem.builder().name("Blueberry Cheesecake Slice").price(new BigDecimal("110")).category(cakes)
                        .description("Creamy New York style cheesecake with blueberry compote").preparationTimeMinutes(2).stockQuantity(15).build(),
                MenuItem.builder().name("Red Velvet Slice").price(new BigDecimal("95")).category(cakes)
                        .description("Moist red velvet cake with cream cheese frosting").preparationTimeMinutes(2).stockQuantity(18).build(),
                MenuItem.builder().name("Tiramisu Cup").price(new BigDecimal("120")).category(cakes)
                        .description("Classic Italian dessert with mascarpone and espresso").preparationTimeMinutes(2).stockQuantity(12).build(),

                // Beverages
                MenuItem.builder().name("Café Latte").price(new BigDecimal("80")).category(beverages)
                        .isVeg(true).description("Smooth espresso with steamed milk").preparationTimeMinutes(5).featured(true).stockQuantity(100).build(),
                MenuItem.builder().name("Hot Chocolate").price(new BigDecimal("75")).category(beverages)
                        .isVeg(true).description("Rich, creamy Belgian hot chocolate").preparationTimeMinutes(5).stockQuantity(100).build(),
                MenuItem.builder().name("Cold Coffee").price(new BigDecimal("90")).category(beverages)
                        .isVeg(true).description("Chilled coffee blended with milk and ice cream").preparationTimeMinutes(5).stockQuantity(100).build(),
                MenuItem.builder().name("Fresh Orange Juice").price(new BigDecimal("70")).category(beverages)
                        .isVeg(true).description("Freshly squeezed orange juice").preparationTimeMinutes(5).stockQuantity(50).build(),

                // Snacks
                MenuItem.builder().name("Veg Sandwich").price(new BigDecimal("55")).category(snacks)
                        .description("Toasted sandwich with fresh veggies and cheese").preparationTimeMinutes(8).featured(true).stockQuantity(40).build(),
                MenuItem.builder().name("Samosa (2 pcs)").price(new BigDecimal("30")).category(snacks)
                        .description("Crispy fried samosas with mint chutney").preparationTimeMinutes(5).stockQuantity(60).build(),
                MenuItem.builder().name("Veg Puff").price(new BigDecimal("25")).category(snacks)
                        .description("Flaky pastry puff stuffed with spiced vegetables").preparationTimeMinutes(3).stockQuantity(50).build(),
                MenuItem.builder().name("Cookie Box (4 pcs)").price(new BigDecimal("80")).category(snacks)
                        .description("Assorted freshly baked cookies — choco chip, oatmeal, and more").preparationTimeMinutes(2).stockQuantity(30).build()
            );

            menuItemRepo.saveAll(items);

            log.info("✅ Seeded {} menu items across {} categories.", items.size(), 5);
            // Kiosk service account — used by the self-order terminal (auto-login, hidden from customers)
            userRepo.save(User.builder()
                    .name("Kiosk Terminal")
                    .email("kiosk@bakeryq.com")
                    .phone("9000000003")
                    .password(passwordEncoder.encode("kiosk@123#secure"))
                    .role(User.Role.STUDENT)
                    .build());

            log.info("✅ Admin login  → admin@bakeryq.com / admin123");
            log.info("✅ Student login → student@test.com / test123");
            log.info("✅ Kiosk account → kiosk@bakeryq.com (auto-used by kiosk terminal)");
        };
    }
}