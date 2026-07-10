package de.open4me.depot.gui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

/**
 * Nicht blockierende MessageBox fuer SWT/Jameica.
 * Im Gegensatz zu {@link org.eclipse.swt.widgets.MessageBox#open()} laeuft der
 * aufrufende Code sofort weiter. Der Rueckgabewert kann spaeter ueber das
 * {@link Handle} abgefragt werden.
 */
public class NonBlockingMessageBox
{
	private final I18N i18n = Application.getI18n();

	private final String title;
	private final String text;
	private final int style;

	public NonBlockingMessageBox(String title, String text, int style)
	{
		this.title = title;
		this.text = text;
		this.style = style;
	}

	/**
	 * Komfort-Methode zum direkten Anzeigen der Box.
	 * @param title Titel der Box.
	 * @param text Inhalt der Box.
	 * @param style SWT-Style-Bits, z.B. {@code SWT.ICON_WARNING | SWT.YES | SWT.NO}.
	 * @return Handle zum spaeteren Schliessen und Auslesen des Rueckgabewerts.
	 */
	public static Handle show(String title, String text, int style)
	{
		return new NonBlockingMessageBox(title, text, style).open();
	}

	/**
	 * Zeigt die MessageBox nicht blockierend an.
	 * @return Handle zum Steuern der Box.
	 */
	public Handle open()
	{
		final Handle handle = new Handle();
		final Display display = GUI.getDisplay();

		display.asyncExec(new Runnable()
		{
			public void run()
			{
				if (handle.isClosed())
					return;

				try
				{
					Shell parent = GUI.getShell();
					final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE);
					handle.attach(shell);

					shell.setText(title == null ? "" : title);
					shell.setLayout(new GridLayout(1, false));

					createContent(shell);
					createButtons(shell, handle);

					shell.pack();
					shell.setMinimumSize(380, shell.getSize().y);
					if (shell.getSize().x < 460)
						shell.setSize(460, shell.getSize().y);

					centerOnParent(shell, parent);
					shell.open();
					shell.forceActive();
				}
				catch (Exception e)
				{
					Logger.error("unable to open non-blocking message box", e);
					handle.close(SWT.CLOSE);
				}
			}
		});

		return handle;
	}

	private void createContent(Composite parent)
	{
		Image icon = getIcon(parent.getDisplay());
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		body.setLayout(new GridLayout(icon != null ? 2 : 1, false));

		if (icon != null)
		{
			Label image = new Label(body, SWT.NONE);
			image.setImage(icon);
			image.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		}

		Label message = new Label(body, SWT.WRAP);
		message.setText(text == null ? "" : text);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, true);
		gd.widthHint = 420;
		message.setLayoutData(gd);
	}

	private void createButtons(Composite parent, final Handle handle)
	{
		List<ButtonSpec> buttons = getButtons();
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

		RowLayout layout = new RowLayout();
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.spacing = 8;
		area.setLayout(layout);

		org.eclipse.swt.widgets.Button defaultButton = null;
		for (ButtonSpec spec : buttons)
		{
			org.eclipse.swt.widgets.Button button = new org.eclipse.swt.widgets.Button(area, SWT.PUSH);
			button.setText(spec.label);
			button.setLayoutData(new RowData(110, SWT.DEFAULT));
			button.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e)
				{
					handle.close(spec.returnCode);
				}
			});

			if (spec.isDefault)
				defaultButton = button;
		}

		if (defaultButton != null && parent.getShell() != null && !parent.getShell().isDisposed())
			parent.getShell().setDefaultButton(defaultButton);
	}

	private List<ButtonSpec> getButtons()
	{
		List<ButtonSpec> buttons = new ArrayList<ButtonSpec>();

		if ((style & SWT.YES) != 0)
			buttons.add(new ButtonSpec(i18n.tr("Ja"), SWT.YES, false));
		if ((style & SWT.NO) != 0)
			buttons.add(new ButtonSpec(i18n.tr("Nein"), SWT.NO, false));
		if ((style & SWT.OK) != 0)
			buttons.add(new ButtonSpec(i18n.tr("OK"), SWT.OK, false));
		if ((style & SWT.CANCEL) != 0)
			buttons.add(new ButtonSpec(i18n.tr("Abbrechen"), SWT.CANCEL, false));

		if (buttons.isEmpty())
			buttons.add(new ButtonSpec(i18n.tr("OK"), SWT.OK, true));
		else
			buttons.get(0).isDefault = true;

		return buttons;
	}

	private Image getIcon(Display display)
	{
		if ((style & SWT.ICON_ERROR) != 0)
			return display.getSystemImage(SWT.ICON_ERROR);
		if ((style & SWT.ICON_WARNING) != 0)
			return display.getSystemImage(SWT.ICON_WARNING);
		if ((style & SWT.ICON_INFORMATION) != 0)
			return display.getSystemImage(SWT.ICON_INFORMATION);
		if ((style & SWT.ICON_QUESTION) != 0)
			return display.getSystemImage(SWT.ICON_QUESTION);
		if ((style & SWT.ICON_WORKING) != 0)
			return display.getSystemImage(SWT.ICON_WORKING);
		return null;
	}

	private void centerOnParent(Shell shell, Shell parent)
	{
		Rectangle parentBounds = parent != null && !parent.isDisposed() ? parent.getBounds() : GUI.getDisplay().getPrimaryMonitor().getBounds();
		Rectangle shellBounds = shell.getBounds();

		int x = parentBounds.x + ((parentBounds.width - shellBounds.width) / 2);
		int y = parentBounds.y + ((parentBounds.height - shellBounds.height) / 2);
		shell.setLocation(x, y);
	}

	private static class ButtonSpec
	{
		private final String label;
		private final int returnCode;
		private boolean isDefault;

		private ButtonSpec(String label, int returnCode, boolean isDefault)
		{
			this.label = label;
			this.returnCode = returnCode;
			this.isDefault = isDefault;
		}
	}

	public interface CloseListener
	{
		void closed(int returnCode);
	}

	/**
	 * Handle fuer die Kontrolle einer nicht blockierenden MessageBox.
	 */
	public static class Handle
	{
		private final AtomicBoolean closed = new AtomicBoolean(false);
		private final AtomicReference<Integer> returnCode = new AtomicReference<Integer>(null);
		private final AtomicReference<Shell> shell = new AtomicReference<Shell>(null);
		private final List<CloseListener> listeners = new CopyOnWriteArrayList<CloseListener>();

		private void attach(final Shell shell)
		{
			this.shell.set(shell);

			shell.addListener(SWT.Close, new Listener()
			{
				public void handleEvent(Event event)
				{
					finish(SWT.CLOSE);
				}
			});

			shell.addDisposeListener(new DisposeListener()
			{
				public void widgetDisposed(DisposeEvent e)
				{
					Handle.this.shell.compareAndSet(shell, null);
					finish(SWT.CLOSE);
				}
			});
		}

		/**
		 * Schliesst die Box ohne Benutzerentscheidung.
		 * Der Rueckgabewert wird dabei auf {@link SWT#CLOSE} gesetzt.
		 */
		public void close()
		{
			close(SWT.CLOSE);
		}

		/**
		 * Schliesst die Box und setzt den Rueckgabewert.
		 * @param returnCode Rueckgabewert, z.B. {@link SWT#YES}.
		 */
		public void close(final int returnCode)
		{
			if (!finish(returnCode))
				return;

			final Shell current = this.shell.get();
			if (current == null || current.isDisposed())
				return;

			Display display = current.getDisplay();
			Runnable job = new Runnable()
			{
				public void run()
				{
					try
					{
						if (!current.isDisposed())
							current.close();
					}
					catch (Exception e)
					{
						Logger.error("unable to close non-blocking message box", e);
					}
				}
			};

			if (Display.getCurrent() == display)
				job.run();
			else
				display.asyncExec(job);
		}

		/**
		 * Liefert true, wenn die Box bereits geschlossen wurde.
		 * @return true, wenn geschlossen.
		 */
		public boolean isClosed()
		{
			return closed.get();
		}

		/**
		 * Liefert den Rueckgabewert der Box oder {@code null}, solange noch
		 * keine Entscheidung getroffen wurde.
		 * @return Rueckgabewert oder {@code null}.
		 */
		public Integer getReturnCode()
		{
			return returnCode.get();
		}

		/**
		 * Liefert true, sobald ein Rueckgabewert verfuegbar ist.
		 * @return true, wenn ein Rueckgabewert vorhanden ist.
		 */
		public boolean hasReturnCode()
		{
			return returnCode.get() != null;
		}

		/**
		 * Registriert einen Listener, der genau einmal beim Schliessen
		 * der Box aufgerufen wird.
		 * @param listener Listener.
		 */
		public void addCloseListener(CloseListener listener)
		{
			if (listener == null)
				return;

			this.listeners.add(listener);
			if (isClosed() && hasReturnCode())
				listener.closed(returnCode.get().intValue());
		}

		private boolean finish(int code)
		{
			if (!returnCode.compareAndSet(null, Integer.valueOf(code)))
				return false;

			closed.set(true);
			for (CloseListener listener : listeners)
			{
				try
				{
					listener.closed(code);
				}
				catch (Exception e)
				{
					Logger.error("error while notifying non-blocking message box listener", e);
				}
			}
			return true;
		}
	}
}
