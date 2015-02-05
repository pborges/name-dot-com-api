import com.google.gson.JsonObject;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pborges on 2/4/2015.
 */
public class NameDotComAPI {
    private Logger log = Logger.getLogger(NameDotComAPI.class);
    private String sessionToken;
    private String baseUrl = "https://www.name.com/api";

    public NameDotComAPI(String username, String apiKey) throws UnirestException {
        sessionToken = getSessionToken(username, apiKey);
    }

    private String getSessionToken(String username, String apiKey) throws UnirestException {
        log.debug("getSessionToken(" + username + ", " + apiKey + ");");
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("username", username);
        jsonRequest.addProperty("api_token", apiKey);
        log.debug("REQUEST(getSessionToken): " + jsonRequest.toString());
        JsonNode jsonResponse = Unirest.post(baseUrl + "/login")
                .header("accept", "application/json")
                .body(jsonRequest.toString())
                .asJson().getBody();
        log.debug("RESPONSE(getSessionToken): " + jsonResponse.toString());
        return jsonResponse.getObject().get("session_token").toString();
    }

    public void createDNSRecord(String hostname, String domain, String ip) throws UnirestException {
        log.debug("createDNSRecord(" + hostname + ", " + domain + "," + ip + ");");
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("hostname", hostname);
        jsonRequest.addProperty("type", "A");
        jsonRequest.addProperty("ttl", 300);
        jsonRequest.addProperty("content", ip);

        log.debug("REQUEST(createDNSRecord): " + jsonRequest.toString());
        JsonNode jsonResponse = Unirest.post(baseUrl + "/dns/create/" + domain)
                .header("accept", "application/json")
                .header("Api-Session-Token", sessionToken)
                .body(jsonRequest.toString())
                .asJson().getBody();
        log.debug("RESPONSE(createDNSRecord): " + jsonResponse.toString());
    }

    public void deleteDNSRecord(NameDNSRecord dnsRecord) throws UnirestException {
        log.debug("deleteDNSRecord(" + dnsRecord.getRecordId() + ");");
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("record_id", dnsRecord.getRecordId());


        log.debug("REQUEST(deleteDNSRecord): " + jsonRequest.toString());
        JsonNode jsonResponse = Unirest.post(baseUrl + "/dns/delete/" + dnsRecord.getDomain())
                .header("accept", "application/json")
                .header("Api-Session-Token", sessionToken)
                .body(jsonRequest.toString())
                .asJson().getBody();
        log.debug("RESPONSE(deleteDNSRecord): " + jsonResponse.toString());
    }

    public List<NameDNSRecord> getDnsRecords(String domain) throws UnirestException, ParseException {
        log.debug("getDnsRecords(" + domain + ");");
        log.debug("REQUEST(getDnsRecords)");
        JsonNode jsonResponse = Unirest.get(baseUrl + "/dns/list/" + domain)
                .header("Api-Session-Token", sessionToken)
                .asJson().getBody();
        log.debug("RESPONSE(getDnsRecords): " + jsonResponse.toString());

        List<NameDNSRecord> results = new ArrayList<NameDNSRecord>();

        JSONArray records = jsonResponse.getObject().getJSONArray("records");
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            NameDNSRecord nameDNSRecord = new NameDNSRecord();
            nameDNSRecord.setRecordId((record.get("record_id") == JSONObject.NULL ? null : Integer.parseInt(record.getString("record_id"))));
            nameDNSRecord.setName((record.get("name") == JSONObject.NULL ? null : record.getString("name")));
            nameDNSRecord.setType(record.get("type") == JSONObject.NULL ? null : record.getString("type"));
            nameDNSRecord.setContent((record.get("content") == JSONObject.NULL ? null : record.getString("content")));
            nameDNSRecord.setTtl((record.get("ttl") == JSONObject.NULL ? null : Integer.parseInt(record.getString("ttl"))));
            nameDNSRecord.setCreateDate((record.get("create_date") == JSONObject.NULL ? null : record.getString("create_date")));
            nameDNSRecord.setDomain(domain);
            results.add(nameDNSRecord);
        }
        return results;
    }

    public void updateIp(String hostname, String domain, String ip) throws ParseException, UnirestException {
        log.debug("updateIp(" + hostname + ", " + domain + "," + ip + ");");
        for (NameDNSRecord record : getDnsRecords(domain)) {
            if (record.getName().equals(hostname + "." + domain)) {
                deleteDNSRecord(record);
                break;
            }
        }
        createDNSRecord(hostname, domain, ip);
    }

    public String getExternalIp() throws UnirestException {
        log.debug("getExternalIp();");

        log.debug("REQUEST(getExternalIp)");
        return Unirest.get("http://checkip.amazonaws.com").asString().getBody();
    }
}
