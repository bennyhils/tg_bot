package org.example;

import bennyhils.inc.tgbot.action.Buy;
import bennyhils.inc.tgbot.model.receipt.Amount;
import bennyhils.inc.tgbot.model.receipt.Item;
import bennyhils.inc.tgbot.model.receipt.Receipt;
import bennyhils.inc.tgbot.util.DataTimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Locale;

/**
 * Unit test for simple App.
 */

@Slf4j
public class AppTest
        extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public void testDate() {
        LocalDateTime localDateTime = LocalDateTime.now();
        Instant instant = Instant.now();

        Instant now1 = Instant.now();
        Instant plus = now1.plus(1, ChronoUnit.DAYS);
        DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy", Locale.forLanguageTag("ru-RU"));
        System.out.println(localDateTime.format(DataTimeUtil.DATE_TIME_FORMATTER));
    }

    public void testJSONWriter() {
        String rub = "RUB";
        String descriptionForProvData = "Доступ к VPN. " + "На " + Buy.SIX_MONTHS + " месяцев";
        Amount amount = new Amount(Buy.SIX_MONTHS_PRICE / 100 + ".00", rub);
        Item item = new Item(descriptionForProvData, "1.00", amount, 1);
        Receipt receipt = new Receipt("mail@mail.ru", Collections.singletonList(item));

        ObjectWriter ow = new ObjectMapper().writer().withRootName("receipt").withDefaultPrettyPrinter();
        String json;
        try {
            json = ow.writeValueAsString(receipt);
            log.info(json);
        } catch (JsonProcessingException e) {
            log.error("Не удалось преобразовать объект в JSON: {}", e.getMessage());
        }
    }

    public void testServerSelect() {
        int serversSize = 3;
        int clientsCount = 100;

        for (int i = 1; i <= clientsCount; i++) {
            log.info("Selected server is '{}'", (i - 1) % serversSize);
        }
    }

    public void testSendMigrationMessages() {

    }
}
