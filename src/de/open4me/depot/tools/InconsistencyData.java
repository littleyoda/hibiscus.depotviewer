package de.open4me.depot.tools;

import java.math.BigDecimal;

/**
 * Datenstruktur für eine Bestand-Orderbuch Inkonsistenz
 */
public class InconsistencyData {
    
    private String kontoId;
    private String kontoName;
    private Integer wpId;
    private String wertpapiername;
    private String wkn;
    private String isin;
    private BigDecimal bestandAnzahl;
    private BigDecimal orderbuchAnzahl;
    private BigDecimal differenz;
    
    public InconsistencyData(String kontoId, String kontoName, Integer wpId, 
                           String wertpapiername, String wkn, String isin,
                           BigDecimal bestandAnzahl, BigDecimal orderbuchAnzahl) {
        this.kontoId = kontoId;
        this.kontoName = kontoName;
        this.wpId = wpId;
        this.wertpapiername = wertpapiername;
        this.wkn = wkn;
        this.isin = isin;
        this.bestandAnzahl = bestandAnzahl;
        this.orderbuchAnzahl = orderbuchAnzahl;
        this.differenz = bestandAnzahl.subtract(orderbuchAnzahl);
    }
    
    // Getters
    public String getKontoId() { return kontoId; }
    public String getKontoName() { return kontoName; }
    public Integer getWpId() { return wpId; }
    public String getWertpapiername() { return wertpapiername != null ? wertpapiername : ""; }
    public String getWkn() { return wkn != null ? wkn : ""; }
    public String getIsin() { return isin != null ? isin : ""; }
    public BigDecimal getBestandAnzahl() { return bestandAnzahl; }
    public BigDecimal getOrderbuchAnzahl() { return orderbuchAnzahl; }
    public BigDecimal getDifferenz() { return differenz; }
    
    /**
     * @return true wenn Bestand < Orderbuch (fehlt Verkauf im Orderbuch)
     */
    public boolean needsSale() {
        return differenz.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * @return true wenn Bestand > Orderbuch (fehlt Kauf im Orderbuch)
     */
    public boolean needsPurchase() {
        return differenz.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * @return Anzahl die gekauft/verkauft werden muss um Konsistenz herzustellen
     */
    public BigDecimal getRequiredAmount() {
        return differenz.abs();
    }
    
    /**
     * @return "B" für Kauf oder "S" für Verkauf
     */
    public String getRequiredAction() {
        return needsPurchase() ? "B" : "S";
    }
    
    @Override
    public String toString() {
        return String.format("Konto: %s, WP: %s (%s), Bestand: %s, Orderbuch: %s, Differenz: %s",
                kontoName, getWertpapiername(), getWkn(), 
                bestandAnzahl.toPlainString(), orderbuchAnzahl.toPlainString(), 
                differenz.toPlainString());
    }
}