package de.open4me.depot.tools.io.feldConverter;

import java.util.ArrayList;

import de.open4me.depot.tools.io.feldConverter.options.FeldConverterOption;

public abstract class FeldConverter<T> {

	private String beschreibung;
	private ArrayList<FeldConverterOption> opts = new ArrayList<FeldConverterOption>(); 

	public FeldConverter(String beschreibung) {
		this.beschreibung = beschreibung;
	}
	
	public abstract Object convert(String roh) throws Exception;
	
	@Override
	public String toString() {
		return beschreibung;
	}
	
	public void addOptions(FeldConverterOption o) {
		opts.add(o);
	}
	
	public ArrayList<FeldConverterOption> getOptions() {
		return opts;
	}
	
}
