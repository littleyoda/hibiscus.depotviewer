package de.open4me.depot.reports;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.open4me.depot.reports.DepotviewerReportObjects.DepotBestandProxy;
import de.open4me.depot.reports.DepotviewerReportObjects.DepotOrderProxy;
import de.open4me.depot.reports.DepotviewerReportObjects.DepotProxy;
import de.open4me.depot.reports.DepotviewerReportObjects.KursProxy;
import de.open4me.depot.reports.DepotviewerReportObjects.WertpapierProxy;

public class DepotviewerMcpToolProvider
{
    private final DepotviewerReportObjects objects = new DepotviewerReportObjects();

    public String getNamespace()
    {
        return "depotviewer";
    }

    public List<Map<String, Object>> getTools()
    {
        return Arrays.asList(
            tool("depots_list", "Depots auflisten", "Listet aktive oder alle Depots.",
                schema("scope", "string", "limit", "integer")),
            tool("portfolio_list", "Portfolio auflisten", "Listet Depot-Bestaende, optional fuer Depot und Stichtag.",
                schema("depotId", "string", "date", "string", "limit", "integer")),
            tool("orders_list", "Orderbuch auflisten", "Listet Depot-Orders mit Zeitraum- und Limit-Filtern.",
                schema("depotId", "string", "from", "string", "to", "string", "lastDays", "integer",
                    "limit", "integer")),
            tool("securities_list", "Wertpapiere auflisten", "Listet Wertpapiere im Bestand oder alle Wertpapiere.",
                schema("scope", "string", "limit", "integer")),
            tool("security_price_get", "Wertpapierkurs abrufen", "Liefert den Kurs eines Wertpapiers am Stichtag.",
                schema("securityId", "string", "date", "string")),
            tool("security_prices_list", "Wertpapierkurse auflisten", "Listet Kurse eines Wertpapiers.",
                schema("securityId", "string", "from", "string", "to", "string", "lastDays", "integer",
                    "limit", "integer")));
    }

    public Object call(String localToolName, Map<String, Object> arguments)
    {
        if ("depots_list".equals(localToolName))
            return depots(arguments);
        if ("portfolio_list".equals(localToolName))
            return portfolio(arguments);
        if ("orders_list".equals(localToolName))
            return orders(arguments);
        if ("securities_list".equals(localToolName))
            return securities(arguments);
        if ("security_price_get".equals(localToolName))
            return securityPrice(arguments);
        if ("security_prices_list".equals(localToolName))
            return securityPrices(arguments);
        throw new IllegalArgumentException("Unbekanntes Depotviewer-Tool: " + localToolName);
    }

    private Map<String, Object> depots(Map<String, Object> arguments)
    {
        Iterable<DepotProxy> source = "all".equalsIgnoreCase(string(arguments.get("scope")))
            ? objects.getDepots().getAlle().limit(limit(arguments))
            : objects.getDepots().getAktive().limit(limit(arguments));
        List<Map<String, Object>> depots = new ArrayList<>();
        for (DepotProxy depot : source)
            depots.add(depot(depot));
        return result("depots", depots);
    }

    private Map<String, Object> portfolio(Map<String, Object> arguments)
    {
        String depotId = string(arguments.get("depotId"));
        DepotviewerReportObjects.BestandListProxy source = depotId == null || depotId.isBlank()
            ? objects.getPortfolio()
            : depotById(depotId).getBestand();
        String date = string(arguments.get("date"));
        if (date != null && !date.isBlank())
            source = source.am(date);
        source = source.limit(limit(arguments));

        List<Map<String, Object>> positions = new ArrayList<>();
        for (DepotBestandProxy position : source)
            positions.add(position(position));
        return result("positions", positions);
    }

    private Map<String, Object> orders(Map<String, Object> arguments)
    {
        String depotId = string(arguments.get("depotId"));
        DepotviewerReportObjects.OrderListProxy source = depotId == null || depotId.isBlank()
            ? objects.getOrderbuch()
            : depotById(depotId).getOrderbuch();
        Integer lastDays = integer(arguments.get("lastDays"));
        if (lastDays != null)
            source = source.letzteTage(lastDays);
        String from = string(arguments.get("from"));
        String to = string(arguments.get("to"));
        if (from != null && !from.isBlank() && to != null && !to.isBlank())
            source = source.zeitraum(from, to);
        source = source.limit(limit(arguments));

        List<Map<String, Object>> orders = new ArrayList<>();
        for (DepotOrderProxy order : source)
            orders.add(order(order));
        return result("orders", orders);
    }

    private Map<String, Object> securities(Map<String, Object> arguments)
    {
        Iterable<WertpapierProxy> source = "all".equalsIgnoreCase(string(arguments.get("scope")))
            ? objects.getWertpapiere().getAlle().limit(limit(arguments))
            : objects.getWertpapiere().getBestand().limit(limit(arguments));
        List<Map<String, Object>> securities = new ArrayList<>();
        for (WertpapierProxy security : source)
            securities.add(security(security));
        return result("securities", securities);
    }

    private Map<String, Object> securityPrice(Map<String, Object> arguments)
    {
        WertpapierProxy security = requireSecurity(arguments);
        KursProxy price = security.kurs(require(arguments, "date"));
        return single("price", price(price));
    }

    private Map<String, Object> securityPrices(Map<String, Object> arguments)
    {
        WertpapierProxy security = requireSecurity(arguments);
        DepotviewerReportObjects.KursListProxy source = security.getKurse();
        Integer lastDays = integer(arguments.get("lastDays"));
        if (lastDays != null)
            source = source.letzteTage(lastDays);
        String from = string(arguments.get("from"));
        String to = string(arguments.get("to"));
        if (from != null && !from.isBlank() && to != null && !to.isBlank())
            source = source.zeitraum(from, to);
        source = source.limit(limit(arguments));

        List<Map<String, Object>> prices = new ArrayList<>();
        for (KursProxy price : source)
            prices.add(price(price));
        return result("prices", prices);
    }

    private DepotProxy depotById(String depotId)
    {
        for (DepotProxy depot : objects.getDepots().getAlle())
        {
            if (depotId.equals(depot.getId()))
                return depot;
        }
        throw new IllegalArgumentException("Depot nicht gefunden: " + depotId);
    }

    private WertpapierProxy requireSecurity(Map<String, Object> arguments)
    {
        String id = require(arguments, "securityId");
        for (WertpapierProxy security : objects.getWertpapiere().getAlle())
        {
            if (id.equals(security.getId()))
                return security;
        }
        throw new IllegalArgumentException("Wertpapier nicht gefunden: " + id);
    }

    private Map<String, Object> depot(DepotProxy depot)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", depot.getId());
        result.put("name", depot.getName());
        result.put("kontonummer", depot.getKontonummer());
        result.put("blz", depot.getBlz());
        result.put("iban", depot.getIban());
        result.put("aktiv", depot.getAktiv());
        result.put("offline", depot.getOffline());
        return result;
    }

    private Map<String, Object> position(DepotBestandProxy position)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", position.getId());
        result.put("depotId", position.getDepotId());
        result.put("wertpapier", security(position.getWertpapier()));
        result.put("anzahl", decimal(position.getAnzahl()));
        result.put("kurs", decimal(position.getKurs()));
        result.put("kurswaehrung", position.getKurswaehrung());
        result.put("wert", decimal(position.getWert()));
        result.put("wertwaehrung", position.getWertwaehrung());
        result.put("datum", position.getDatum());
        result.put("bewertungsdatum", position.getBewertungsdatum());
        return result;
    }

    private Map<String, Object> order(DepotOrderProxy order)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("depotId", order.getDepotId());
        result.put("wertpapier", security(order.getWertpapier()));
        result.put("aktion", order.getAktion());
        result.put("anzahl", decimal(order.getAnzahl()));
        result.put("kurs", decimal(order.getKurs()));
        result.put("kurswaehrung", order.getKurswaehrung());
        result.put("kosten", decimal(order.getKosten()));
        result.put("kostenwaehrung", order.getKostenwaehrung());
        result.put("gebuehren", decimal(order.getGebuehren()));
        result.put("gebuehrenwaehrung", order.getGebuehrenwaehrung());
        result.put("steuern", decimal(order.getSteuern()));
        result.put("steuernwaehrung", order.getSteuernwaehrung());
        result.put("buchungsdatum", order.getBuchungsdatum());
        result.put("kommentar", order.getKommentar());
        return result;
    }

    private Map<String, Object> security(WertpapierProxy security)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", security.getId());
        result.put("name", security.getName());
        result.put("wkn", security.getWkn());
        result.put("isin", security.getIsin());
        result.put("kurs", decimal(security.getKurs()));
        result.put("kursdatum", security.getKursdatum());
        return result;
    }

    private Map<String, Object> price(KursProxy price)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        if (price == null)
            return result;
        result.put("wert", decimal(price.getWert()));
        result.put("waehrung", price.getWaehrung());
        result.put("datum", price.getDatum());
        return result;
    }

    private static Map<String, Object> tool(String name, String title, String description,
                                           Map<String, Object> inputSchema)
    {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("title", title);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        return tool;
    }

    private static Map<String, Object> schema(Object... pairs)
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
        {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", pairs[i + 1]);
            properties.put(String.valueOf(pairs[i]), property);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    private static Map<String, Object> result(String key, List<Map<String, Object>> values)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, values);
        result.put("count", values.size());
        return result;
    }

    private static Map<String, Object> single(String key, Map<String, Object> value)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }

    private static String require(Map<String, Object> arguments, String key)
    {
        String value = string(arguments.get(key));
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Argument fehlt: " + key);
        return value;
    }

    private static int limit(Map<String, Object> arguments)
    {
        Integer limit = integer(arguments.get("limit"));
        return limit == null ? Integer.MAX_VALUE : Math.max(0, limit);
    }

    private static Integer integer(Object value)
    {
        if (value instanceof Number)
            return ((Number) value).intValue();
        if (value == null)
            return null;
        return Integer.parseInt(String.valueOf(value));
    }

    private static String string(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal decimal(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
