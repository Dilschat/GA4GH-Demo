package test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.elixir.ega.ebi.dataedge.DataEdgeServiceApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = DataEdgeServiceApplication.class)
@WebAppConfiguration
@AutoConfigureMockMvc
public class ENATicketControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    public void ticketRetreivingTestWithOneLinkToENA() throws Exception {
        String actualTicket = mvc.perform(get("/ga4gh/sample/SAMN07666497").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String expectedTicket = new String(java.nio.file.Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/expectedTicket.json")));
        JSONAssert.assertEquals(expectedTicket,actualTicket, true);
    }

    @Test
    public void ticketRetreivingTestWithMultipleLinksToENA() throws Exception {
        String actualTicket = mvc.perform(get("/ga4gh/sample/SAMN07666417").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        System.out.println(actualTicket);
        String expectedTicket = new String(java.nio.file.Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/expectedjson1.json")));
        JSONAssert.assertEquals(expectedTicket,actualTicket, true);
    }
}