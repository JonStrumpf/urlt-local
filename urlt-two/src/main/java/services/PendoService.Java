package services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PendoClient {

    public enum Events {
        LOGIN("login"),
        LOGOFF("logoff"),
        REF_CENTER_SEARCH("refCenterSearch");

        String description;

        Events(String description) {

            this.description = description;
        }

        @Override
        public String toString() {

            return this.description;
        }
    }

    protected static final String ACCOUNT_ID_KEY = "accountId";
    protected String domain;
    protected String path;
    protected APIProtocol protocol;
    protected String secret;
    protected ICustomer customer;

    public PendoClient(ICustomer customer, Persistence persistence) throws ValidationException {

        Preconditions.checkValuesNotNull(customer, persistence);

        this.customer = customer;

        CacheKey cacheKey = new CacheKey(getClass().getSimpleName(), CacheKey.DEFAULT_TIME_TO_LIVE);
        List<String> delimitedValues = (List<String>) Cache.getInstance().get(cacheKey.getKey(), customer);

        if (BaseConverter.isNullOrEmpty(delimitedValues)) {
            delimitedValues = persistence.queryForList("select sed_field_name || '|' || sed_field_value from service " +
                    "join servicedetail on sed_se_num = se_num and sed_component = 'trackevents' " +
                    "where se_co_num = 0 and se_active = 't' and se_type = 'api' and se_id = 'pendo'", String.class);
            if (!BaseConverter.isNullOrEmpty(delimitedValues)) {
                Cache.getInstance().set(cacheKey.getKey(), CacheKey.DEFAULT_TIME_TO_LIVE, delimitedValues);
            }
        }

        setApiDataFromDelimitedList(delimitedValues);
    }

    protected void setApiDataFromDelimitedList(List<String> delimitedValues) {

        if (!BaseConverter.isNullOrEmpty(delimitedValues)) {
            delimitedValues.forEach(delimitedValue -> {
                String[] values = delimitedValue.split("\\|");
                if (values.length == 2) {
                    String name = values[0];
                    String value = values[1];

                    switch (name) {
                        case "protocol":
                            protocol = APIProtocol.valueOf(value);
                            break;
                        case "domain":
                            domain = value;
                            break;
                        case "path":
                            path = value;
                            break;
                        case "sharedsecret":
                            secret = BaseConverter.DB2Name(value);
                            break;
                        default:
                    }
                }
            });
        }
    }

    private String getVisitorId() {

        // This should really tack on "-ghost" for ghost members, but do NOT want to add on that pre AE
        return Strings.nullToEmpty(customer.getUserNum());
    }

    // accountId suffixes are defined within "app.pendo.io -> exclude & include lists" to reduce MAU counts
    private String getAccountId() {

        String accountId = Objects.toString(customer.getCurrentCompany(), "");
        // excludes non-prod environments
        if (BaseConverter.isPartnerQAEnvironment()) {
            accountId += "-" + UtilClass.getEnvironment();
        }
        // exclude demo/qa/inactive companies
        if ("100".equals(customer.getCurrentCompanyMailbox())) {
            accountId += "-demo";
        } else if ("101".equals(customer.getCurrentCompanyMailbox())) {
            accountId += "-qa";
        } else if ("999".equals(customer.getCurrentCompanyMailbox())) {
            accountId += "-inactive";
        }

        return accountId;
    }

    protected Map<String, Object> buildPayload(String event, Map<String, Object> properties) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("visitorId", getVisitorId());
        payload.put(ACCOUNT_ID_KEY, getAccountId());
        payload.put("type", "track");
        payload.put("timestamp", BaseConverter.getSqlTimestamp().getTime());
        if (BaseConverter.isNullOrEmpty(properties)) {
            properties = new HashMap<>();
        }
        properties.put("environment", UtilClass.getEnvironment());
        payload.put("properties", properties);

        Map<String, Object> context = new HashMap<>();
        context.put("ip", customer.getIP());
        context.put("userAgent", customer.getUserAgent());
        context.put("url", UtilClass.getBenefitsolverDomain() + "/benefits/BenefitSolverView");
        payload.put("context", context);

        return payload;
    }

    protected Map<String, String> buildHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("x-pendo-integration-key", secret);

        return headers;
    }

    protected APIRequest makeRequest(Map<String, String> headers, Map<String, Object> payload) {

        return new APIRequest.Builder(APIRequestMethod.POST)
                .protocol(protocol)
                .domain(domain)
                .path(path)
                .authorization(new NoAuthAuthorization())
                .contentType(APIRequest.MIMEType.JSON)
                .additionalHeaders(headers)
                .payload(new Gson().toJson(payload))
                .build();
    }

    public void track(String event) {

        track(event, null);
    }

    public void track(String event, Map<String, Object> properties) {
        track(event, properties, true);
    }


    // get a hit to our track endpoint with some params
    // mm_num = 3428972394
    // company = 3942984923
    // comapign = 342492384
    // Map map = new map
    // map.set(mm_num, 9482948234)///

    //PendoClient.trackEvent("URL Click", map);

    public void track(String event, Map<String, Object> properties, boolean canRetry) {

        if (Strings.isNullOrEmpty(customer.getUserNum())
                || customer.getCurrentCompany() == null
                || protocol == null
                || Strings.isNullOrEmpty(domain)
                || Strings.isNullOrEmpty(path)
                || Strings.isNullOrEmpty(secret)) {
            return;
        }

        Map<String, String> headers = buildHeaders();
        Map<String, Object> payload = buildPayload(event, properties);
        try (APIRequest request = makeRequest(headers, payload)) {

            APIResponse response = request.execute(false);
            handleUnsuccessfulApiResponse(response, event, properties, canRetry);
        } catch (APIResponseException e) {
            handleUnsuccessfulApiResponse(e.getApiResponse(), event, properties, canRetry);
        } catch (Exception e) {
            String message = event + ": " + (Strings.isNullOrEmpty(e.getMessage()) ? UtilClass.getStackTrace(e) : e.getMessage());
            Log.getInstance().error(getClass().getSimpleName(), message);
        }
    }

    private void handleUnsuccessfulApiResponse(APIResponse apiResponse, String event, Map<String, Object> properties, boolean canRetry) {

        Integer statusCode = apiResponse == null ? null : apiResponse.getStatusCode();
        if (statusCode != null && statusCode != 200) {

            // immediate retry for specific pendo server errors - https://developers.pendo.io/docs/?bash#status-codes
            if ((statusCode.equals(502) || statusCode.equals(503)) && canRetry) {
                track(event, properties, false);
                return;
            }

            Log.getInstance().warn(getClass().getSimpleName(), "Event: " + event
                    + " | User: " + customer.getUserNum()
                    + " | HTTP response code: " + apiResponse.getStatusCode() + " for URL: " + protocol.getValue() + "://" + domain + path
                    + " | Retried: " + !canRetry
                    + " | " + apiResponse.getBody());
        }
    }
}
