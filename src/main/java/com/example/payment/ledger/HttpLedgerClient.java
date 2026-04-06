package com.example.payment.ledger;

import com.example.payment.model.BalanceResponse;
import com.example.payment.model.TransferResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class HttpLedgerClient implements LedgerClient {

    private final RestClient restClient;

    /**
     * Production constructor — wired by Spring with configuration values.
     *
     * {@code @Autowired} is required because the class has two constructors.
     * Without it, Spring 6 cannot determine which to use and throws
     * "No default constructor found" at startup.
     */
    @Autowired
    public HttpLedgerClient(
            @Value("${ledger.base-url}") String baseUrl,
            @Value("${ledger.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${ledger.read-timeout-ms:5000}") int readTimeoutMs) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Package-private test constructor.
     * Accepts an already-configured {@link RestTemplate} so that
     * {@link org.springframework.test.web.client.MockRestServiceServer} can intercept calls.
     * Never invoked by Spring — only called directly in tests.
     */
    HttpLedgerClient(RestTemplate restTemplate, String baseUrl) {
        this.restClient = RestClient.create(restTemplate).mutate()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public BalanceResponse getBalance(UUID accountId) {
        try {
            var raw = restClient.get()
                    .uri("/api/ledger/accounts/{id}/balance", accountId)
                    .retrieve()
                    .body(BalanceRaw.class);
            return new BalanceResponse(raw.accountId(), raw.availableBalance(), raw.currency());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new LedgerException("Account not found: " + accountId);
            }
            throw new LedgerException("Ledger client error getting balance: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new LedgerException("Ledger server error getting balance: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new LedgerException("Ledger unreachable (getBalance): " + e.getMessage(), e);
        }
    }

    @Override
    public TransferResponse executeTransfer(UUID fromAccountId, UUID toAccountId,
                                            BigDecimal amount, String currency) {
        var request = new TransferRequest(fromAccountId, toAccountId, amount, currency);
        try {
            var raw = restClient.post()
                    .uri("/api/ledger/transfers")
                    .body(request)
                    .retrieve()
                    .body(TransferRaw.class);
            return new TransferResponse(raw.transferId(), raw.status());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new InvalidAccountException("Invalid account in transfer request");
            }
            throw new LedgerException("Ledger client error executing transfer: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new LedgerException("Ledger server error executing transfer (status="
                    + e.getStatusCode() + "): " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new LedgerException("Ledger unreachable (executeTransfer): " + e.getMessage(), e);
        }
    }

    // ── Private wrappers ─────────────────────────────────────────────────────

    private record TransferRequest(UUID fromAccountId, UUID toAccountId,
                                   BigDecimal amount, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BalanceRaw(UUID accountId, BigDecimal availableBalance, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransferRaw(String transferId, String status) {}
}
