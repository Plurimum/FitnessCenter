package ru.itmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmo.model.Action;
import ru.itmo.model.AddedVisit;
import ru.itmo.model.FromTo;
import ru.itmo.model.Statistic;
import ru.itmo.model.Ticket;
import ru.itmo.model.Type;
import ru.itmo.model.User;

import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {FitnessCenterApplication.class})
@ContextConfiguration(classes = FitnessCenterTests.Initializer.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FitnessCenterTests {
    @Container
    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13")
            .withPassword("123")
            .withUsername("maxim.likhanov")
            .withInitScript("schema.sql");

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(applicationContext.getEnvironment());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    private int userId;
    private String userName;

    @BeforeEach
    public void prepare() {
        this.userName = UUID.randomUUID().toString();
        this.userId = registerUser(userName);
    }

    @AfterEach
    public void cleanUp() {
        try (Connection connection = dataSource.getConnection()){
            connection.prepareStatement("delete from events where true;")
                    .execute();
        } catch (SQLException e) {
            throw new RuntimeException(":(", e);
        }
    }

    @Test
    public void testExtendTicketForTwoDays() {
        postAction(Type.EXTEND, new AddedVisit(2));
    }

    @Test
    public void testCancelAndIgnoreExtendTicket() {
        postAction(Type.CANCEL, null);
    }

    @Test
    public void testPassToFitness() {
        postAction(Type.EXTEND, new AddedVisit(2));
        assertTrue(enterFitness());

        leaveFitness();
        checkTicket(1);
    }

    @Test
    public void testStatistic() {
        postAction(Type.EXTEND, new AddedVisit(2));

        enterFitness();
        leaveFitness();
        enterFitness();
        leaveFitness();

        final Statistic statistic = getStatistic();

        assertEquals(2, statistic.countTimes());
    }

    @Test
    public void testCountOfVisitsInInterval() {
        postAction(Type.EXTEND, new AddedVisit(3));

        enterFitness();
        leaveFitness();
        enterFitness();
        leaveFitness();
        enterFitness();
        leaveFitness();

        int count = getCountInterval(
                new FromTo(
                        Instant.now().minus(3, ChronoUnit.HOURS),
                        Instant.now().plus(1, ChronoUnit.HOURS)
                )
        );

        assertEquals(3, count);

        int countOutOfInterval = getCountInterval(
                new FromTo(
                        Instant.now().minus(3, ChronoUnit.HOURS),
                        Instant.now().minus(1, ChronoUnit.HOURS)
                )
        );

        assertEquals(0, countOutOfInterval);
    }

    private int registerUser(String userName) {
        return Integer.parseInt(
                getPostContent(
                        "/v1/register",
                        new User(userName)
                )
        );
    }

    public void postAction(Type type, Object data) {
        try {
            final Action action = new Action(
                    userId,
                    type,
                    objectMapper.writeValueAsString(data)
            );

            post("/v1/action", action);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unlucky", e);
        }
    }

    public String getPostContent(String path, Object serializableContent) {
        try {
            return post(path, serializableContent)
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unlucky", e);
        }
    }

    private void checkTicket(int expectedVisit) {
        final Ticket ticket = getTicket();

        assertEquals(userName, ticket.user());
        assertEquals(expectedVisit, ticket.count());
    }

    private Ticket getTicket() {
        try {
            return objectMapper.readValue(
                    getPostContent("/v1/get/session-ticket", userId),
                    Ticket.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("bruh", e);
        }

    }

    private ResultActions post(String path, Object serializableContent) {
        try {
            final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(serializableContent));

            return mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unlucky", e);
        } catch (Exception e) {
            throw new RuntimeException("mega unlucky", e);
        }
    }

    private boolean enterFitness() {
        try {
            final String resultContent = post("/v1/in", userId)
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            return Boolean.parseBoolean(resultContent);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unlucky", e);
        }
    }

    private void leaveFitness() {
        postAction(Type.END_VISIT, null);
    }

    private int getCountInterval(FromTo fromTo) {
        return Integer.parseInt(getPostContent("/v1/count/visits", fromTo));
    }

    private Statistic getStatistic() {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/v1/statistic/visits/" + userId);

        try {
            final String resultContent = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            return objectMapper.readValue(resultContent, Statistic.class);
        } catch (Exception e) {
            throw new IllegalStateException("unlucky", e);
        }
    }
}
