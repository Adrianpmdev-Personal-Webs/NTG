package com.adrianpmdev.wowforever.features.gamedata.api.controller;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameImagesService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conditional requests of the game data: a browser that already has the data gets a 304 with no body.
 *
 * @author adrianpmdev
 */
class GameDataControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new GameDataController(new GameDataService(), mock(GameImagesService.class)))
            .build();

    @Test
    void sameVersionAnswersNotModified() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/v1/wow-forever/data/talents/warrior"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, private"))
                .andReturn();
        String etag = first.getResponse().getHeader(HttpHeaders.ETAG);

        MvcResult second = mockMvc.perform(get("/api/v1/wow-forever/data/talents/warrior")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn();

        assertThat(first.getResponse().getContentAsString()).isNotEmpty();
        assertThat(second.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void otherVersionGetsTheData() throws Exception {
        mockMvc.perform(get("/api/v1/wow-forever/data/talents/warrior").header(HttpHeaders.IF_NONE_MATCH, "\"old\""))
                .andExpect(status().isOk());
    }
}
