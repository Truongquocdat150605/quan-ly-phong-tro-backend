public class IntegrationTest {
    @SpringBootTest
@AutoConfigureMockMvc
public class IntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreateRentalRequest() throws Exception {
        String json = "{\"roomId\": 1, \"fullName\": \"Test\", \"phone\": \"0901234567\"}";
        mockMvc.perform(post("/api/public/rental-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk()); // Nếu trả về 200 thì luồng này OK
    }
}
}
