package com.poc.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTest {

    @Autowired MockMvc mvc;

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanListSeededProducts() throws Exception {
        mvc.perform(get("/api/products").with(httpBasic("user", "user123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.name == 'Laptop')]").exists());
    }

    @Test
    void userCannotCreateProduct() throws Exception {
        mvc.perform(post("/api/products").with(httpBasic("user", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Monitor\",\"price\":5000,\"stock\":5}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateProductAndValidationWorks() throws Exception {
        mvc.perform(post("/api/products").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Monitor\",\"price\":5000,\"stock\":5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());

        mvc.perform(post("/api/products").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"price\":0,\"stock\":5}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDecreaseMoreThanStock() throws Exception {
        mvc.perform(post("/api/products/1/decrease-stock").with(httpBasic("user", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1000}"))
            .andExpect(status().isConflict());
    }
}
