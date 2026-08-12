package de.open4me.depot.gui.dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.open4me.depot.DepotViewerPlugin;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung;
import de.willuhn.jameica.hbci.rmi.SepaDauerauftrag;
import de.willuhn.jameica.hbci.rmi.Turnus;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public final class AboutDepotviewerDialog extends AbstractDialog<Void>
{
    private static final String CHANGELOG_FILE = "ChangeLog.txt";
    private static final String LIBRARIES_RESOURCE = "help/de_de/depotviewer-libraries.txt";
    private static final String PAYPAL_URL = "https://www.paypal.com/paypalme/littleyoda/";
    private static final int[] DONATION_IBAN = {
        17, 16, 108, 102, 100, 101, 101, 100, 103, 102, 97, 96, 101, 97, 97, 108, 109, 108, 103, 100,
        101, 100
    };
    private static final String DONATION_NAME = "Sven Bursch-Osewold";
    private static final String DONATION_PURPOSE = "Spende hibiscus.depotviewer";
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 650;

    public AboutDepotviewerDialog()
    {
        super(POSITION_CENTER);
        setTitle("Über Depot-Viewer");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    @Override
    protected void paint(Composite parent) throws Exception
    {
        parent.setLayout(new GridLayout(1, false));

        TabFolder tabs = new TabFolder(parent, SWT.NONE);
        tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        addTextTab(tabs, "Änderungen", readChangeLog());
        addLibrariesTab(tabs);
        addDonationTab(tabs);

        ButtonArea buttons = new ButtonArea();
        buttons.addButton("Schließen", context -> close(), null, true, "ok.png");
        buttons.paint(parent);

        getShell().setMinimumSize(getShell().computeSize(WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private static void addTextTab(TabFolder tabs, String title, String value)
    {
        Composite composite = tabComposite(tabs, title);
        Text text = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL
            | SWT.READ_ONLY);
        text.setText(value);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    private static void addLibrariesTab(TabFolder tabs)
    {
        Composite composite = tabComposite(tabs, "Libraries");
        Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        addColumn(table, "Name", 210);
        addColumn(table, "Homepage", 250);
        addColumn(table, "Lizenz", 170);
        addColumn(table, "Lizenz-URL", 250);

        for (String line : readResource(LIBRARIES_RESOURCE).split("\\R"))
        {
            if (line.isBlank() || line.startsWith("#"))
                continue;
            String[] values = line.split(";", -1);
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] {
                value(values, 0),
                value(values, 1),
                value(values, 3),
                value(values, 2)
            });
        }
    }

    private void addDonationTab(TabFolder tabs) throws Exception
    {
        Composite composite = tabComposite(tabs, "Spenden");
        ScrolledComposite scroller = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);

        Composite content = new Composite(scroller, SWT.NONE);
        content.setLayout(SWTUtil.createGrid(1, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        headline(content, "Kontaktmöglichkeiten");
        label(content, "Du kannst mich über die folgende E-Mail-Adresse erreichen:\n\n"
            + "sb_github2697@bursch.com\n\n");

        headline(content, "Danke sagen");
        label(content, "Falls euch eins meiner Projekte gefallen hat oder ich euch helfen konnte,\n"
            + "würde ich mich über eine kleine Spende freuen...");

        ButtonArea buttons = new ButtonArea();
        buttons.addButton("PayPal öffnen", context -> Program.launch(PAYPAL_URL), null, false, "internet-web-browser.png");
        buttons.addButton("Dauerauftrag", new Action()
        {
            @Override
            public void handleAction(Object context) throws ApplicationException
            {
                close();
                createStandingOrder();
            }
        }, null, false, "dauerauftrag.png");
        buttons.addButton("Überweisung", new Action()
        {
            @Override
            public void handleAction(Object context) throws ApplicationException
            {
                close();
                createTransfer();
            }
        }, null, false, "ueberweisung.png");
        buttons.paint(content);

        scroller.setContent(content);
        scroller.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private static Composite tabComposite(TabFolder tabs, String title)
    {
        TabItem item = new TabItem(tabs, SWT.NONE);
        item.setText(title);
        Composite composite = new Composite(tabs, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        item.setControl(composite);
        return composite;
    }

    private static void headline(Composite parent, String value)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(value);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private static void label(Composite parent, String value)
    {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(value);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private static void addColumn(Table table, String title, int width)
    {
        TableColumn column = new TableColumn(table, SWT.LEFT);
        column.setText(title);
        column.setWidth(width);
    }

    private static String value(String[] values, int index)
    {
        return index < values.length ? values[index] : "";
    }

    private static String readChangeLog()
    {
        try
        {
            String pluginDir = Application.getPluginLoader().getPlugin(DepotViewerPlugin.class).getManifest()
                .getPluginDir();
            Path changelog = Path.of(pluginDir, CHANGELOG_FILE);
            if (Files.isReadable(changelog))
                return Files.readString(changelog, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            Logger.warn("unable to read plugin changelog: " + e.getMessage());
        }

        Path fallback = Path.of(CHANGELOG_FILE);
        try
        {
            return Files.readString(fallback, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            return "Die Datei " + fallback + " konnte nicht gelesen werden: " + e.getMessage();
        }
    }

    private static String readResource(String resource)
    {
        try (InputStream stream = AboutDepotviewerDialog.class.getClassLoader().getResourceAsStream(resource))
        {
            if (stream == null)
                return "";
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            Logger.warn("unable to read resource " + resource + ": " + e.getMessage());
            return "";
        }
    }

    private static void createStandingOrder() throws ApplicationException
    {
        try
        {
            SepaDauerauftrag order = Settings.getDBService().createObject(SepaDauerauftrag.class, null);
            order.setGegenkontoNummer(donationIban());
            order.setGegenkontoName(DONATION_NAME);
            order.setZweck(DONATION_PURPOSE);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 7);
            order.setErsteZahlung(calendar.getTime());

            Turnus turnus = Settings.getDBService().createObject(Turnus.class, null);
            turnus.setIntervall(1);
            turnus.setTag(calendar.get(Calendar.DAY_OF_MONTH));
            turnus.setZeiteinheit(Turnus.ZEITEINHEIT_MONATLICH);
            order.setTurnus(turnus);

            new de.willuhn.jameica.hbci.gui.action.SepaDauerauftragNew().handleAction(order);
        }
        catch (Exception e)
        {
            Logger.error("unable to create donation standing order", e);
            Application.getMessagingFactory().sendMessage(new StatusBarMessage(
                "Fehler beim Anlegen des SEPA-Dauerauftrages: " + e.getMessage(),
                StatusBarMessage.TYPE_ERROR));
        }
    }

    private static void createTransfer() throws ApplicationException
    {
        try
        {
            AuslandsUeberweisung transfer = Settings.getDBService().createObject(AuslandsUeberweisung.class, null);
            transfer.setGegenkontoNummer(donationIban());
            transfer.setGegenkontoName(DONATION_NAME);
            transfer.setZweck(DONATION_PURPOSE);
            new de.willuhn.jameica.hbci.gui.action.AuslandsUeberweisungNew().handleAction(transfer);
        }
        catch (Exception e)
        {
            Logger.error("unable to create donation transfer", e);
            Application.getMessagingFactory().sendMessage(new StatusBarMessage(
                "Fehler beim Anlegen der SEPA-Überweisung: " + e.getMessage(),
                StatusBarMessage.TYPE_ERROR));
        }
    }

    private static String donationIban()
    {
        StringBuilder iban = new StringBuilder(DONATION_IBAN.length);
        for (int value : DONATION_IBAN)
            iban.append((char) (value ^ 0x55));
        return iban.toString();
    }

    @Override
    protected Void getData()
    {
        return null;
    }
}
