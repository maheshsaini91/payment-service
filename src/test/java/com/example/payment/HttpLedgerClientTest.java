//package com.example.payment;
//
//import com.example.payment.ledger.HttpLedgerClient;
//import com.example.payment.ledger.InvalidAccountException;
//import com.example.payment.ledger.LedgerException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.client.MockRestServiceServer;
//import org.springframework.web.client.RestTemplate;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
//import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
//
///**
// * Unit tests for {@link HttpLedgerClient} using {@link MockRestServiceServer}.
// *
// * Note: HttpLedgerClient uses Spring's RestClient. We expose a constructor that
// * accepts a RestTemplate so MockRestServiceServer can intercept calls.
// * The production constructor (base-url + timeouts) is exercised by integration tests.
// */
//class HttpLedgerClientTest {
//
//    private static final String BASE_URL = "http://ledger-test";
//    private static final ObjectMapper MAPPER = new ObjectMapper();
//
//    private MockRestServiceServer server;
//    private HttpLedgerClient client;
//
//    @BeforeEach
//    void setUp() {
//        var restTemplate = new RestTemplate();
//        server = MockRestServiceServer.createServer(restTemplate);
//        client = new HttpLedgerClient(restTemplate, BASE_URL);
//    }
//
//    // ── getBalance ───────────────────────────────────────────────────────────
//
//    @Test
//    void getBalance_returnsBalance_on200() throws Exception {
//        UUID accountId = UUID.randomUUID();
//        var body = MAPPER.writeValueAsString(new BalanceStub(accountId, new BigDecimal("1200.00"), "AED"));
//
//        server.expect(requestTo(BASE_URL + "/api/ledger/accounts/" + accountId + "/balance"))
//                .andExpect(method(HttpMethod.GET))
//                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
//
//        var result = client.getBalance(accountId);
//
//        assertThat(result.availableBalance()).isEqualByComparingTo("1200.00");
//        assertThat(result.currency()).isEqualTo("AED");
//        server.verify();
//    }
//
//    @Test
//    void getBalance_throwsLedgerException_on404() {
//        UUID accountId = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/accounts/" + accountId + "/balance"))
//                .andRespond(withStatus(HttpStatus.NOT_FOUND));
//
//        assertThatThrownBy(() -> client.getBalance(accountId))
//                .isInstanceOf(LedgerException.class)
//                .hasMessageContaining("Account not found");
//    }
//
//    @Test
//    void getBalance_throwsLedgerException_on500() {
//        UUID accountId = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/accounts/" + accountId + "/balance"))
//                .andRespond(withServerError());
//
//        assertThatThrownBy(() -> client.getBalance(accountId))
//                .isInstanceOf(LedgerException.class);
//    }
//
//    @Test
//    void getBalance_throwsLedgerException_on503() {
//        UUID accountId = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/accounts/" + accountId + "/balance"))
//                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
//
//        assertThatThrownBy(() -> client.getBalance(accountId))
//                .isInstanceOf(LedgerException.class);
//    }
//
//    // ── executeTransfer ──────────────────────────────────────────────────────
//
//    @Test
//    void executeTransfer_returnsTransferResponse_on200() throws Exception {
//        UUID from = UUID.randomUUID();
//        UUID to = UUID.randomUUID();
//        var body = MAPPER.writeValueAsString(new TransferStub("txn-99", "SUCCESS"));
//
//        server.expect(requestTo(BASE_URL + "/api/ledger/transfers"))
//                .andExpect(method(HttpMethod.POST))
//                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
//
//        var result = client.executeTransfer(from, to, new BigDecimal("150.00"), "AED");
//
//        assertThat(result.transferId()).isEqualTo("txn-99");
//        assertThat(result.status()).isEqualTo("SUCCESS");
//        server.verify();
//    }
//
//    @Test
//    void executeTransfer_throwsInvalidAccountException_on400() {
//        UUID from = UUID.randomUUID();
//        UUID to = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/transfers"))
//                .andRespond(withBadRequest().body("{\"error\":\"INVALID_ACCOUNT\"}")
//                        .contentType(MediaType.APPLICATION_JSON));
//
//        assertThatThrownBy(() -> client.executeTransfer(from, to, new BigDecimal("50.00"), "AED"))
//                .isInstanceOf(InvalidAccountException.class);
//    }
//
//    @Test
//    void executeTransfer_throwsLedgerException_on500() {
//        UUID from = UUID.randomUUID();
//        UUID to = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/transfers"))
//                .andRespond(withServerError());
//
//        assertThatThrownBy(() -> client.executeTransfer(from, to, new BigDecimal("50.00"), "AED"))
//                .isInstanceOf(LedgerException.class);
//    }
//
//    @Test
//    void executeTransfer_throwsLedgerException_on504() {
//        UUID from = UUID.randomUUID();
//        UUID to = UUID.randomUUID();
//        server.expect(requestTo(BASE_URL + "/api/ledger/transfers"))
//                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
//
//        assertThatThrownBy(() -> client.executeTransfer(from, to, new BigDecimal("50.00"), "AED"))
//                .isInstanceOf(LedgerException.class);
//    }
//
//    // ── Stubs ────────────────────────────────────────────────────────────────
//
//    record BalanceStub(UUID accountId, BigDecimal availableBalance, String currency) {}
//    record TransferStub(String transferId, String status) {}
//}
