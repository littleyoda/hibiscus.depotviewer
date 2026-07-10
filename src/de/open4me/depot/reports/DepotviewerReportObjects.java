package de.open4me.depot.reports;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLQueries;
import de.open4me.depot.sql.SQLUtils;
import de.open4me.depot.sql.SQLUtils.PreparedSQL;
import de.open4me.depot.tools.Bestandsabfragen;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.rmi.Konto;

public class DepotviewerReportObjects
{
    private DepotListProxy depots;
    private WertpapierListProxy wertpapiere;
    private OrderListProxy orderbuch;
    private BestandListProxy portfolio;

    public DepotListProxy getDepots()
    {
        if (depots == null)
            depots = new DepotListProxy(false, null);
        return depots;
    }

    public WertpapierListProxy getWertpapiere()
    {
        if (wertpapiere == null)
            wertpapiere = new WertpapierListProxy(false, null);
        return wertpapiere;
    }

    public OrderListProxy getOrderbuch()
    {
        if (orderbuch == null)
            orderbuch = new OrderListProxy(new OrderQuery(null, null, null, null));
        return orderbuch;
    }

    public BestandListProxy getPortfolio()
    {
        if (portfolio == null)
            portfolio = new BestandListProxy(null, null, null);
        return portfolio;
    }

    private static abstract class ListProxy<T, SELF extends ListProxy<T, SELF>> implements Iterable<T>
    {
        private final Integer limit;
        private List<T> values;

        ListProxy(Integer limit)
        {
            this.limit = normalizeLimit(limit);
        }

        @Override
        public Iterator<T> iterator()
        {
            return asList().iterator();
        }

        public int size()
        {
            return asList().size();
        }

        public boolean isEmpty()
        {
            return asList().isEmpty();
        }

        public List<T> asList()
        {
            if (values == null)
                values = Collections.unmodifiableList(applyLimit(load()));
            return values;
        }

        public SELF limit(int limit)
        {
            return copyWithLimit(limit);
        }

        protected Integer limitValue()
        {
            return limit;
        }

        protected abstract List<T> load();

        protected abstract SELF copyWithLimit(Integer limit);

        private List<T> applyLimit(List<T> source)
        {
            if (source == null || source.isEmpty())
                return Collections.emptyList();
            if (limit == null || limit >= source.size())
                return new ArrayList<>(source);
            return new ArrayList<>(source.subList(0, limit));
        }

        private static Integer normalizeLimit(Integer limit)
        {
            if (limit == null)
                return null;
            return Math.max(0, limit);
        }
    }

    public static final class DepotListProxy extends ListProxy<DepotProxy, DepotListProxy>
    {
        private final boolean all;
        private DepotListProxy aktive;
        private DepotListProxy alle;

        DepotListProxy(boolean all, Integer limit)
        {
            super(limit);
            this.all = all;
        }

        public DepotListProxy getAktive()
        {
            if (aktive == null)
                aktive = new DepotListProxy(false, limitValue());
            return aktive;
        }

        public DepotListProxy getAlle()
        {
            if (alle == null)
                alle = new DepotListProxy(true, limitValue());
            return alle;
        }

        @Override
        protected List<DepotProxy> load()
        {
            return DepotData.depots(!all);
        }

        @Override
        protected DepotListProxy copyWithLimit(Integer limit)
        {
            return new DepotListProxy(all, limit);
        }
    }

    public static final class WertpapierListProxy extends ListProxy<WertpapierProxy, WertpapierListProxy>
    {
        private final boolean all;
        private WertpapierListProxy bestand;
        private WertpapierListProxy alle;

        WertpapierListProxy(boolean all, Integer limit)
        {
            super(limit);
            this.all = all;
        }

        public WertpapierListProxy getBestand()
        {
            if (bestand == null)
                bestand = new WertpapierListProxy(false, limitValue());
            return bestand;
        }

        public WertpapierListProxy getAlle()
        {
            if (alle == null)
                alle = new WertpapierListProxy(true, limitValue());
            return alle;
        }

        @Override
        protected List<WertpapierProxy> load()
        {
            return DepotData.wertpapiere(!all);
        }

        @Override
        protected WertpapierListProxy copyWithLimit(Integer limit)
        {
            return new WertpapierListProxy(all, limit);
        }
    }

    public static final class BestandListProxy extends ListProxy<DepotBestandProxy, BestandListProxy>
    {
        private final String depotId;
        private final Date datum;

        BestandListProxy(String depotId, Date datum, Integer limit)
        {
            super(limit);
            this.depotId = depotId;
            this.datum = datum;
        }

        public BestandListProxy am(Object datum)
        {
            return new BestandListProxy(depotId, DateParser.parse(datum), limitValue());
        }

        @Override
        protected List<DepotBestandProxy> load()
        {
            return DepotData.bestand(depotId, datum);
        }

        @Override
        protected BestandListProxy copyWithLimit(Integer limit)
        {
            return new BestandListProxy(depotId, datum, limit);
        }
    }

    public static final class OrderListProxy extends ListProxy<DepotOrderProxy, OrderListProxy>
    {
        private final OrderQuery query;

        OrderListProxy(OrderQuery query)
        {
            super(query.limit);
            this.query = query;
        }

        public OrderListProxy letzteTage(int days)
        {
            int safeDays = Math.max(0, days);
            LocalDate to = LocalDate.now();
            return new OrderListProxy(query.withFrom(DateParser.fromLocalDate(to.minusDays(safeDays)))
                .withTo(DateParser.fromLocalDate(to)));
        }

        public OrderListProxy zeitraum(String from, String to)
        {
            return new OrderListProxy(query.withFrom(DateParser.parse(from)).withTo(DateParser.parse(to)));
        }

        @Override
        protected List<DepotOrderProxy> load()
        {
            return DepotData.orderbuch(query);
        }

        @Override
        protected OrderListProxy copyWithLimit(Integer limit)
        {
            return new OrderListProxy(query.withLimit(limit));
        }
    }

    public static final class KursListProxy extends ListProxy<KursProxy, KursListProxy>
    {
        private final KursQuery query;

        KursListProxy(KursQuery query)
        {
            super(query.limit);
            this.query = query;
        }

        public KursListProxy letzteTage(int days)
        {
            int safeDays = Math.max(0, days);
            LocalDate to = LocalDate.now();
            return new KursListProxy(query.withFrom(DateParser.fromLocalDate(to.minusDays(safeDays)))
                .withTo(DateParser.fromLocalDate(to)));
        }

        public KursListProxy zeitraum(String from, String to)
        {
            return new KursListProxy(query.withFrom(DateParser.parse(from)).withTo(DateParser.parse(to)));
        }

        @Override
        protected List<KursProxy> load()
        {
            return DepotData.kurse(query);
        }

        @Override
        protected KursListProxy copyWithLimit(Integer limit)
        {
            return new KursListProxy(query.withLimit(limit));
        }
    }

    public static final class DepotProxy
    {
        private final Konto konto;
        private BestandListProxy bestand;
        private OrderListProxy orderbuch;

        DepotProxy(Konto konto)
        {
            this.konto = konto;
        }

        public String getId()
        {
            return value(() -> konto.getID());
        }

        public String getName()
        {
            String name = value(() -> konto.getBezeichnung());
            if (name == null || name.trim().isEmpty())
                name = value(() -> konto.getLongName());
            return text(name);
        }

        public String getKontonummer()
        {
            return text(value(() -> konto.getKontonummer()));
        }

        public String getBlz()
        {
            return text(value(() -> konto.getBLZ()));
        }

        public String getIban()
        {
            return text(value(() -> konto.getIban()));
        }

        public boolean getOffline()
        {
            return flag(Konto.FLAG_OFFLINE);
        }

        public boolean getAktiv()
        {
            return !flag(Konto.FLAG_DISABLED);
        }

        public BestandListProxy getBestand()
        {
            if (bestand == null)
                bestand = new BestandListProxy(getId(), null, null);
            return bestand;
        }

        public BestandListProxy bestand(Object datum)
        {
            return getBestand().am(datum);
        }

        public OrderListProxy getOrderbuch()
        {
            if (orderbuch == null)
                orderbuch = new OrderListProxy(new OrderQuery(getId(), null, null, null));
            return orderbuch;
        }

        private boolean flag(int flag)
        {
            return Boolean.TRUE.equals(value(() -> konto.hasFlag(flag)));
        }
    }

    public static final class DepotBestandProxy
    {
        private final GenericObjectSQL object;
        private WertpapierProxy wertpapier;

        DepotBestandProxy(GenericObjectSQL object)
        {
            this.object = object;
        }

        public String getId()
        {
            Object id = attribute("id");
            if (id != null)
                return text(id);
            try
            {
                return text(object.getID());
            }
            catch (Exception e)
            {
                return "";
            }
        }

        public String getDepotId()
        {
            return text(attribute("kontoid"));
        }

        public WertpapierProxy getWertpapier()
        {
            if (wertpapier == null)
                wertpapier = DepotData.wertpapier(text(attribute("wpid")), object);
            return wertpapier;
        }

        public BigDecimal getAnzahl()
        {
            return decimal(attribute("anzahl"));
        }

        public BigDecimal getKurs()
        {
            return decimal(attribute("kurs"));
        }

        public String getKurswaehrung()
        {
            return text(attribute("kursw"));
        }

        public BigDecimal getWert()
        {
            return decimal(attribute("wert"));
        }

        public String getWertwaehrung()
        {
            return text(attribute("wertw"));
        }

        public Date getDatum()
        {
            return date(attribute("datum"));
        }

        public Date getBewertungsdatum()
        {
            Date datum = date(attribute("bewertungszeitpunkt"));
            return datum == null ? date(attribute("kursdatum")) : datum;
        }

        public Object get(String name)
        {
            return attribute(name);
        }

        public Object attribute(String name)
        {
            return DepotData.attribute(object, name);
        }
    }

    public static final class DepotOrderProxy
    {
        private final GenericObjectSQL object;
        private WertpapierProxy wertpapier;

        DepotOrderProxy(GenericObjectSQL object)
        {
            this.object = object;
        }

        public String getId()
        {
            Object id = attribute("umsatzid");
            return text(id == null ? value(() -> object.getID()) : id);
        }

        public String getDepotId()
        {
            return text(attribute("kontoid"));
        }

        public WertpapierProxy getWertpapier()
        {
            if (wertpapier == null)
                wertpapier = DepotData.wertpapier(text(attribute("wpid")), object);
            return wertpapier;
        }

        public String getAktion()
        {
            return text(attribute("aktion"));
        }

        public BigDecimal getAnzahl()
        {
            return decimal(attribute("anzahl"));
        }

        public BigDecimal getKurs()
        {
            return decimal(attribute("kurs"));
        }

        public String getKurswaehrung()
        {
            return text(attribute("kursw"));
        }

        public BigDecimal getKosten()
        {
            return decimal(attribute("kosten"));
        }

        public String getKostenwaehrung()
        {
            return text(attribute("kostenw"));
        }

        public BigDecimal getGebuehren()
        {
            return decimal(attribute("transaktionskosten"));
        }

        public String getGebuehrenwaehrung()
        {
            return text(attribute("transaktionskostenw"));
        }

        public BigDecimal getSteuern()
        {
            return decimal(attribute("steuern"));
        }

        public String getSteuernwaehrung()
        {
            return text(attribute("steuernw"));
        }

        public Date getBuchungsdatum()
        {
            return date(attribute("buchungsdatum"));
        }

        public String getKommentar()
        {
            return text(attribute("kommentar"));
        }

        public Object get(String name)
        {
            return attribute(name);
        }

        public Object attribute(String name)
        {
            return DepotData.attribute(object, name);
        }
    }

    public static final class WertpapierProxy
    {
        private final GenericObjectSQL object;
        private KursListProxy kurse;

        WertpapierProxy(GenericObjectSQL object)
        {
            this.object = object;
        }

        public String getId()
        {
            Object id = attribute("id");
            if (id == null)
                id = attribute("wpid");
            if (id != null)
                return text(id);
            try
            {
                return text(object.getID());
            }
            catch (Exception e)
            {
                return "";
            }
        }

        public String getName()
        {
            return text(attribute("wertpapiername"));
        }

        public String getWkn()
        {
            return text(attribute("wkn"));
        }

        public String getIsin()
        {
            return text(attribute("isin"));
        }

        public BigDecimal getKurs()
        {
            return decimal(attribute("kurs"));
        }

        public Date getKursdatum()
        {
            return date(attribute("kursdatum"));
        }

        public KursProxy kurs(Object datum)
        {
            return DepotData.kurs(getId(), DateParser.parse(datum));
        }

        public KursListProxy getKurse()
        {
            if (kurse == null)
                kurse = new KursListProxy(new KursQuery(getId(), null, null, null));
            return kurse;
        }

        public Object get(String name)
        {
            return attribute(name);
        }

        public Object attribute(String name)
        {
            return DepotData.attribute(object, name);
        }
    }

    public static final class KursProxy
    {
        private final GenericObjectSQL object;

        KursProxy(GenericObjectSQL object)
        {
            this.object = object;
        }

        public BigDecimal getWert()
        {
            return decimal(attribute("kurs"));
        }

        public String getWaehrung()
        {
            return text(attribute("kursw"));
        }

        public Date getDatum()
        {
            return date(attribute("kursdatum"));
        }

        public Object get(String name)
        {
            return attribute(name);
        }

        public Object attribute(String name)
        {
            return DepotData.attribute(object, name);
        }
    }

    private static final class OrderQuery
    {
        private final String depotId;
        private final Date from;
        private final Date to;
        private final Integer limit;

        OrderQuery(String depotId, Date from, Date to, Integer limit)
        {
            this.depotId = depotId;
            this.from = from;
            this.to = to;
            this.limit = limit;
        }

        OrderQuery withFrom(Date from)
        {
            return new OrderQuery(depotId, from, to, limit);
        }

        OrderQuery withTo(Date to)
        {
            return new OrderQuery(depotId, from, to, limit);
        }

        OrderQuery withLimit(Integer limit)
        {
            return new OrderQuery(depotId, from, to, limit);
        }
    }

    private static final class KursQuery
    {
        private final String wertpapierId;
        private final Date from;
        private final Date to;
        private final Integer limit;

        KursQuery(String wertpapierId, Date from, Date to, Integer limit)
        {
            this.wertpapierId = wertpapierId;
            this.from = from;
            this.to = to;
            this.limit = limit;
        }

        KursQuery withFrom(Date from)
        {
            return new KursQuery(wertpapierId, from, to, limit);
        }

        KursQuery withTo(Date to)
        {
            return new KursQuery(wertpapierId, from, to, limit);
        }

        KursQuery withLimit(Integer limit)
        {
            return new KursQuery(wertpapierId, from, to, limit);
        }
    }

    private static final class DepotData
    {
        static List<DepotProxy> depots(boolean onlyActive)
        {
            List<DepotProxy> result = new ArrayList<>();
            try
            {
                DBIterator<Konto> iterator = Settings.getDBService().createList(Konto.class);
                while (iterator.hasNext())
                {
                    Konto konto = iterator.next();
                    if (!Utils.hasRightKontoType(konto))
                        continue;
                    if (onlyActive && konto.hasFlag(Konto.FLAG_DISABLED))
                        continue;
                    result.add(new DepotProxy(konto));
                }
                result.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
                return result;
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Depots konnten nicht geladen werden", e);
            }
        }

        static List<WertpapierProxy> wertpapiere(boolean onlyCurrentHoldings)
        {
            List<GenericObjectSQL> objects = onlyCurrentHoldings
                ? SQLQueries.getOwnedWertpapiereMitKursdatum()
                : SQLQueries.getWertpapiereMitKursdatum();
            return wertpapiere(objects);
        }

        static WertpapierProxy wertpapier(String id, GenericObjectSQL fallback)
        {
            if (id != null && !id.trim().isEmpty())
            {
                try(PreparedSQL sql = SQLUtils.getPreparedSQL("select *, concat(wertpapiername , ' (' , wkn , ' / ' , isin , ')') as nicename from depotviewer_wertpapier where id = ?"))
                {
                    sql.prest.setString(1, id);
                    List<GenericObjectSQL> objects = SQLUtils.getResultSet(sql.prest, "depotviewer_wertpapier", "id", "wertpapiername");
                    if (!objects.isEmpty())
                        return new WertpapierProxy(objects.get(0));
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("Wertpapier konnte nicht geladen werden", e);
                }
            }
            return new WertpapierProxy(fallback);
        }

        static KursProxy kurs(String wertpapierId, Date datum)
        {
            if (wertpapierId == null || wertpapierId.trim().isEmpty() || datum == null)
                return null;
            try(PreparedSQL sql = SQLUtils.getPreparedSQL(
                "select * from depotviewer_kurse where wpid = ? and kursdatum <= ? order by kursdatum desc"))
            {
                sql.prest.setString(1, wertpapierId);
                sql.prest.setDate(2, new java.sql.Date(datum.getTime()));
                List<GenericObjectSQL> objects = SQLUtils.getResultSet(sql.prest, "depotviewer_kurse", "id", "kursdatum");
                return objects.isEmpty() ? null : new KursProxy(objects.get(0));
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Kurs konnte nicht geladen werden", e);
            }
        }

        static List<KursProxy> kurse(KursQuery query)
        {
            try(PreparedSQL sql = SQLUtils.getPreparedSQL(kurseSql(query)))
            {
                int index = 1;
                if (query.wertpapierId != null)
                    sql.prest.setString(index++, query.wertpapierId);
                if (query.from != null)
                    sql.prest.setDate(index++, new java.sql.Date(query.from.getTime()));
                if (query.to != null)
                    sql.prest.setDate(index++, new java.sql.Date(query.to.getTime()));
                List<GenericObjectSQL> objects = SQLUtils.getResultSet(sql.prest, "depotviewer_kurse", "id", "kursdatum");
                List<KursProxy> result = new ArrayList<>();
                for (GenericObjectSQL object : objects)
                    result.add(new KursProxy(object));
                return result;
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Kurse konnten nicht geladen werden", e);
            }
        }

        static List<DepotBestandProxy> bestand(String depotId, Date datum)
        {
            List<GenericObjectSQL> objects;
            try
            {
                objects = Bestandsabfragen.getBestand(datum);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Depot-Bestand konnte nicht geladen werden", e);
            }
            List<DepotBestandProxy> result = new ArrayList<>();
            for (GenericObjectSQL object : objects)
            {
                if (depotId == null || depotId.equals(text(attribute(object, "kontoid"))))
                    result.add(new DepotBestandProxy(object));
            }
            return result;
        }

        static List<DepotOrderProxy> orderbuch(OrderQuery query)
        {
            try(PreparedSQL sql = SQLUtils.getPreparedSQL(orderbuchSql(query)))
            {
                int index = 1;
                if (query.depotId != null)
                    sql.prest.setString(index++, query.depotId);
                if (query.from != null)
                    sql.prest.setDate(index++, new java.sql.Date(query.from.getTime()));
                if (query.to != null)
                    sql.prest.setDate(index++, new java.sql.Date(query.to.getTime()));
                List<GenericObjectSQL> objects = SQLUtils.getResultSet(sql.prest, "depotviewer_umsaetze", "id", "id");
                List<DepotOrderProxy> result = new ArrayList<>();
                for (GenericObjectSQL object : objects)
                    result.add(new DepotOrderProxy(object));
                return result;
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Depot-Orderbuch konnte nicht geladen werden", e);
            }
        }

        static Object attribute(GenericObjectSQL object, String name)
        {
            if (object == null || name == null || name.trim().isEmpty())
                return null;
            try
            {
                return object.getAttribute(name.toLowerCase());
            }
            catch (RemoteException e)
            {
                throw new IllegalStateException("Depotviewer-Objekt konnte nicht gelesen werden", e);
            }
        }

        private static List<WertpapierProxy> wertpapiere(List<GenericObjectSQL> objects)
        {
            List<WertpapierProxy> result = new ArrayList<>();
            if (objects != null)
            {
                for (GenericObjectSQL object : objects)
                    result.add(new WertpapierProxy(object));
            }
            return result;
        }

        private static String orderbuchSql(OrderQuery query)
        {
            StringBuilder sql = new StringBuilder();
            sql.append("select *, ")
                .append("concat(kosten, ' ', kostenw) as joinkosten, ")
                .append("konto.id as kontoid, ")
                .append("depotviewer_umsaetze.id as umsatzid, ")
                .append("concat(steuern, ' ', steuernw) as joinsteuern, ")
                .append("concat(transaktionskosten, ' ', transaktionskostenw) as jointransaktionskosten ")
                .append("from depotviewer_umsaetze ")
                .append("left join depotviewer_wertpapier on depotviewer_umsaetze.wpid = depotviewer_wertpapier.id ")
                .append("left join konto on konto.id = depotviewer_umsaetze.kontoid ");
            List<String> where = new ArrayList<>();
            if (query.depotId != null)
                where.add("depotviewer_umsaetze.kontoid = ?");
            if (query.from != null)
                where.add("buchungsdatum >= ?");
            if (query.to != null)
                where.add("buchungsdatum <= ?");
            appendWhere(sql, where);
            sql.append(" order by buchungsdatum desc");
            return sql.toString();
        }

        private static String kurseSql(KursQuery query)
        {
            StringBuilder sql = new StringBuilder("select * from depotviewer_kurse");
            List<String> where = new ArrayList<>();
            if (query.wertpapierId != null)
                where.add("wpid = ?");
            if (query.from != null)
                where.add("kursdatum >= ?");
            if (query.to != null)
                where.add("kursdatum <= ?");
            appendWhere(sql, where);
            sql.append(" order by kursdatum desc");
            return sql.toString();
        }

        private static void appendWhere(StringBuilder sql, List<String> where)
        {
            if (where.isEmpty())
                return;
            sql.append(" where ");
            for (int i = 0; i < where.size(); i++)
            {
                if (i > 0)
                    sql.append(" and ");
                sql.append(where.get(i));
            }
        }
    }

    private static final class DateParser
    {
        private static final String[] PATTERNS = { "yyyy-MM-dd", "dd.MM.yyyy" };

        static Date parse(Object value)
        {
            if (value == null)
                return null;
            if (value instanceof Date)
                return (Date) value;
            if (value instanceof LocalDate)
                return fromLocalDate((LocalDate) value);
            if (value instanceof CharSequence)
                return parseText(value.toString());
            throw new IllegalArgumentException("Datum wird nicht unterstuetzt: " + value);
        }

        static Date fromLocalDate(LocalDate value)
        {
            return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        private static Date parseText(String value)
        {
            String text = value == null ? "" : value.trim();
            if (text.isEmpty())
                return null;
            for (String pattern : PATTERNS)
            {
                try
                {
                    SimpleDateFormat format = new SimpleDateFormat(pattern);
                    format.setLenient(false);
                    return format.parse(text);
                }
                catch (ParseException e)
                {
                    // Try next supported report date format.
                }
            }
            throw new IllegalArgumentException("Datum muss im Format YYYY-MM-DD angegeben werden: " + value);
        }
    }

    private interface RemoteValue<T>
    {
        T get() throws RemoteException;
    }

    private static <T> T value(RemoteValue<T> value)
    {
        try
        {
            return value.get();
        }
        catch (RemoteException e)
        {
            throw new IllegalStateException("Depotviewer-Objekt konnte nicht gelesen werden", e);
        }
    }

    private static String text(Object value)
    {
        return value == null ? "" : value.toString();
    }

    private static BigDecimal decimal(Object value)
    {
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        String text = value.toString().trim();
        return text.isEmpty() ? null : new BigDecimal(text);
    }

    private static Date date(Object value)
    {
        return value instanceof Date ? (Date) value : null;
    }
}
