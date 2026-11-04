package com.instakill.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadedFileIsStoredAndServed() throws Exception {
        Cookie token = registerAndGetToken("mediauser", "media@example.com");
        byte[] bytes = "fake-mp4-bytes".getBytes();

        MvcResult result = mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "clip.MP4", "video/mp4", bytes))
                        .cookie(token))
                .andExpect(status().isCreated())
                .andReturn();

        String mediaUrl = objectMapper.readTree(result.getResponse().getContentAsString()).get("mediaUrl").asText();
        assertThat(mediaUrl).startsWith("/media/").endsWith(".mp4");

        mockMvc.perform(get(mediaUrl))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes));
    }

    @Test
    void uploadedImageIsResizedToMaxWidth() throws Exception {
        Cookie token = registerAndGetToken("mediauser3", "media3@example.com");
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(900, 450, BufferedImage.TYPE_INT_RGB), "png", encoded);

        MvcResult result = mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", encoded.toByteArray()))
                        .cookie(token))
                .andExpect(status().isCreated())
                .andReturn();

        String mediaUrl = objectMapper.readTree(result.getResponse().getContentAsString()).get("mediaUrl").asText();
        byte[] served = mockMvc.perform(get(mediaUrl)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(served));
        assertThat(stored.getWidth()).isEqualTo(300);
        assertThat(stored.getHeight()).isEqualTo(150);
    }

    @Test
    void rejectsImageExtensionWithNonImageBytes() throws Exception {
        Cookie token = registerAndGetToken("mediauser4", "media4@example.com");

        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3}))
                        .cookie(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedExtensionAndEmptyFile() throws Exception {
        Cookie token = registerAndGetToken("mediauser2", "media2@example.com");

        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1}))
                        .cookie(token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                        .cookie(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})))
                .andExpect(status().isUnauthorized());
    }

    private Cookie registerAndGetToken(String username, String email) throws Exception {
        String payload = objectMapper.writeValueAsString(new AuthPayload(username, email, "password"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        Cookie token = result.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(token).isNotNull();
        return token;
    }

    private record AuthPayload(String username, String email, String password) {
    }
}
