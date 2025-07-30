package net.chamman.moonnight.infra.map.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DaumMapClientTest {

    @Autowired
    private DaumMapClient daumMapClient;

    @Test
    void validateAddress_테스트() {
        boolean result = daumMapClient.validateAddress("04017", "서울 마포구 동교로 23");
        assertTrue(result);
    }
}
