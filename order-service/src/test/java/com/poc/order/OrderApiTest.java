package com.poc.order;

import com.poc.order.client.ProductClient;
import com.poc.order.client.ProductClient.ProductDto;
import com.poc.order.service.DiscountPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired MockMvc mvc;
    @Autowired DiscountPolicy discountPolicy;
    @MockBean ProductClient productClient;   // replace the remote call with a mock bean

    @Test
    void containerInjectsPercentageDiscountFromConfig() {
        assertThat(discountPolicy.name()).isEqualTo("PERCENTAGE_10");
    }

    @Test
    void placeOrderAppliesDiscount() throws Exception {
        ProductDto laptop = new ProductDto(1L, "Laptop", new BigDecimal("1000.00"), 10);
        when(productClient.getProduct(1L)).thenReturn(laptop);
        when(productClient.decreaseStock(eq(1L), any())).thenReturn(laptop);

        mvc.perform(post("/api/orders").with(httpBasic("user", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalPrice").value(1800.00))
            .andExpect(jsonPath("$.username").value("user"));

        mvc.perform(get("/api/orders/my").with(httpBasic("user", "user123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void quantityLimitComesFromConfiguration() throws Exception {
        mvc.perform(post("/api/orders").with(httpBasic("user", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":50}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminSeesAllOrders() throws Exception {
        mvc.perform(get("/api/orders").with(httpBasic("user", "user123"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").with(httpBasic("admin", "admin123"))).andExpect(status().isOk());
    }
}
