package de.open4me.depot.abruf.utils;
//package de.open4me.depot.depotabruf;
//
//import java.io.IOException;
//import java.rmi.RemoteException;
//import java.text.SimpleDateFormat;
//import java.util.Properties;
//
//import org.kapott.hbci.GV_Result.GVRWPDepotUms;
//import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument;
//import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument.Transaction;
//import org.kapott.hbci.exceptions.HBCI_Exception;
//import org.kapott.hbci.status.HBCIMsgStatus;
//import org.kapott.hbci.structures.BigDecimalValue;
//import org.kapott.hbci.structures.TypedValue;
//import org.kapott.hbci.swift.Swift;
//
//import de.willuhn.jameica.hbci.rmi.Konto;
//import de.willuhn.util.ApplicationException;
//import de.willuhn.util.Base64;
//
//
//public class MusterDepot extends HBCIDepot {
//
//	class MyGVUms  {
//		private TypedValue parseTypedValue(String st) {
//			String st_type=st.substring(7,11);
//			String curr="";
//			boolean withCurr = false;
//
//			int saldo_type = -1;
//			if (st_type.equals("FAMT")) { 
//				saldo_type=TypedValue.TYPE_WERT;
//			} else if (st_type.equals("ACTU")) { 
//				saldo_type=TypedValue.TYPE_WERT;
//				withCurr = true;
//			} else if (st_type.equals("UNIT")) {
//				saldo_type=TypedValue.TYPE_STCK;
//			} else if (st_type.equals("PRCT")) {
//				saldo_type=TypedValue.TYPE_PROZENT;
//			}
//			int pos1=12;
//			boolean neg = (st.charAt(pos1)=='N');
//			if (neg)
//				pos1++;    
//			if (withCurr) {
//				curr = st.substring(pos1, pos1+3);
//				pos1 += 3;
//			} 
//			return new TypedValue(
//					(neg?"-":"")+st.substring(pos1).replace(',','.'),
//					curr,
//					saldo_type);
//		}
//
//		GVRWPDepotUms depot;
//
//		// Aus hbci4java übernommen
//		protected void extractResults(HBCIMsgStatus msgstatus,String header,int idx) 
//		{
//			depot = new GVRWPDepotUms();
//			Properties result=msgstatus.getData();
//
//			StringBuffer paramName=new StringBuffer(header).append(".data536");
//			StringBuffer buffer = new StringBuffer();
//			buffer.append(Swift.decodeUmlauts(result.getProperty(paramName.toString())));
//
//
//			final SimpleDateFormat date_time_format = new SimpleDateFormat("yyyyMMdd hhmmss");
//			final SimpleDateFormat date_only_format = new SimpleDateFormat("yyyyMMdd");
//
//			while (buffer.length()!=0) {
//				try {
//					String onerecord=Swift.getOneBlock(buffer);
//
//					GVRWPDepotUms.Entry entry=new GVRWPDepotUms.Entry();
//
//					String st_timestamp=null;
//					String st_date=null;
//					String st_time=null;
//					char option='C';
//					int  i=0;
//
//					while (true) {
//						//Parse allgemeine Informationen (Mandatory Sequence A General Information)
//						st_timestamp=Swift.getTagValue(onerecord,"98"+option,i++);
//						if (st_timestamp==null) {
//							if (option=='C') {
//								option='A';
//								i=0;
//							} else {
//								break;
//							}
//						} else {
//							if (st_timestamp.substring(1,5).equals("PREP")) {
//								st_date=st_timestamp.substring(7,15);
//								if (option=='C') {
//									st_time=st_timestamp.substring(15,21);
//								}
//								break;
//							}
//						}
//					} 
//
//
//					if (st_time!=null) {
//						entry.timestamp=date_time_format.parse(st_date+" "+st_time);
//					} else if (st_date != null) {
//						entry.timestamp=date_only_format.parse(st_date);
//					}
//
//					String st_depot=Swift.getTagValue(onerecord,"97A",0);
//					int pos1=st_depot.indexOf("//");
//					int pos2=st_depot.indexOf("/",pos1+2);
//					if (pos2<0)
//						pos2=st_depot.length();
//					entry.depot= new org.kapott.hbci.structures.Konto();
//					entry.depot.blz=st_depot.substring(pos1+2,pos2);
//					if (pos2 < st_depot.length())
//						entry.depot.number=st_depot.substring(pos2+1);
//					//getMainPassport().fillAccountInfo(entry.depot);
//
//					String st;
//					i=0;
//					// Parse einzelnes Finanzinstrument (Repetitive Optional Subsequence B1 Financial Instrument)
//					st=Swift.getTagValue(onerecord,"17B",0);
//					if (st.substring(st.indexOf("//")+2).equals("Y")) {
//						int fin_start=onerecord.indexOf(":16R:FIN");
//
//						while (true) {
//							int fin_end=onerecord.indexOf(":16S:FIN",fin_start);
//							if ((fin_end)==-1) {
//								break;
//							}
//
//							String oneinstrument=onerecord.substring(fin_start,fin_end+8);
//							fin_start+=oneinstrument.length();
//
//							FinancialInstrument instrument=new GVRWPDepotUms.Entry.FinancialInstrument();
//
//							int trans_start = oneinstrument.indexOf(":16R:TRAN\r\n");
//							String oneinstrument_header;
//							if (trans_start >= 0)
//								oneinstrument_header = oneinstrument.substring(0, trans_start+9);
//							else
//								oneinstrument_header = oneinstrument;
//
//							st=Swift.getTagValue(oneinstrument_header,"35B",0);
//							boolean haveISIN=st.substring(0,5).equals("ISIN ");
//
//							if (haveISIN) {
//								pos1=st.indexOf("\r\n");
//								instrument.isin=st.substring(5,pos1);
//								if (pos1+2<st.length() && st.substring(pos1+2,pos1+6).equals("/DE/")) {
//									pos2=st.indexOf("\r\n",pos1+6);
//									if (pos2==-1) {
//										pos2=st.length();
//									}
//									instrument.wkn=st.substring(pos1+6,pos2);
//									pos1=pos2;
//								}
//							} else {
//								pos1=st.indexOf("\r\n");
//								instrument.wkn=st.substring(4,pos1);
//							}
//
//							pos1+=2;
//							if (pos1<st.length())
//								instrument.name=st.substring(pos1);
//
//							if (instrument.name!=null) {
//								StringBuffer sb=new StringBuffer(instrument.name);
//								int p;
//								while ((p=sb.indexOf("\r\n"))!=-1) {
//									sb.replace(p,p+2," ");
//								}
//								instrument.name=sb.toString();
//							}
//							i=0;
//							while (true) {
//								st=Swift.getTagValue(oneinstrument_header,"93B",i++);
//								if (st==null)
//									break;
//								String qualifier = st.substring(1,5);
//
//								if ("FIOP".equals(qualifier) || (instrument.startSaldo == null && "INOP".equals(qualifier))) {
//									instrument.startSaldo = parseTypedValue(st);
//								} else if ("FICL".equals(qualifier) || (instrument.endSaldo == null && "INCL".equals(qualifier))) {
//									instrument.endSaldo   = parseTypedValue(st);
//								} else {
//									System.out.println("Unbekannter 93B: " + st);
//								}
//							}
//
//							i=0;
//							while (true) {
//								st=Swift.getTagValue(oneinstrument_header,"98A",i++);
//								if (st==null)
//									break;
//								String qualifier = st.substring(1,5);
//
//								if ("PRIC".equals(qualifier)) {
//									instrument.preisdatum = date_only_format.parse(st.substring(7, 15));
//								} else {
//									System.out.println("Unbekannter 98A: " + st);
//								}
//							}
//
//							i=0;
//							while (true) {
//								st=Swift.getTagValue(oneinstrument_header,"90A",i++);
//								if (st==null)
//									break;
//
//								instrument.preis = parseTypedValue(st);
//							}
//
//							i=0;
//							while (true) {
//								st=Swift.getTagValue(oneinstrument_header,"90B",i++);
//								if (st==null)
//									break;
//
//								instrument.preis = parseTypedValue(st);
//							}
//
//							//Parse einzelne Transaktionen 
//							while (trans_start >= 0) {
//								int trans_end = oneinstrument.indexOf(":16S:TRAN\r\n", trans_start);
//								if (trans_end<0)
//									break;
//								String onetransaction = oneinstrument.substring(trans_start, trans_end+9);
//								trans_start=trans_end+9;
//
//								Transaction transaction = new Transaction();
//
//								int link_start = onetransaction.indexOf(":16R:LINK");
//								if (link_start >=0) {
//									int link_end = onetransaction.indexOf(":16S:LINK", link_start);
//									if (link_end >= 0) {
//										String onelink = onetransaction.substring(link_start, link_end+8);
//										String rela = Swift.getTagValue(onelink, "20C", 0);
//
//										if (rela != null) {
//											transaction.kundenreferenz = rela.substring(7);
//										}
//									}
//								}
//
//								int detail_start = onetransaction.indexOf(":16R:TRANSDET");
//								if (detail_start >= 0) {
//									int detail_end = onetransaction.indexOf(":16S:TRANSDET", detail_start);
//									if (detail_end >= 0) {
//										String onedetail = onetransaction.substring(detail_start, detail_end+12);
//
//										String quantity = Swift.getTagValue(onedetail, "36B", 0);
//										if (quantity != null)
//											if (quantity.startsWith(":PSTA")) {
//												transaction.anzahl = parseTypedValue(quantity);
//											} else {
//												System.out.println("Unbekannter 36B: " + quantity);
//											}
//
//										String t99a = Swift.getTagValue(onedetail, "99A", 0);
//										if (t99a != null)
//											if (t99a.startsWith(":DAAC")) {
//												int neg = 0;
//												if (t99a.charAt(7) == 'N')
//													neg = 1;
//												transaction.stueckzins_tage = Integer.parseInt(t99a.substring(7+neg));
//												if (neg != 0)
//													transaction.stueckzins_tage = -transaction.stueckzins_tage;
//											} else {
//												System.out.println("Unbekannter 99A: " + t99a);
//											}
//
//										int tagidx = 0;
//										while (true) {
//											String t19a = Swift.getTagValue(onedetail, "19A", tagidx++);
//											if (t19a == null)
//												break;
//
//											if (t19a.startsWith(":PSTA")) {
//												int off=7;
//												if (t19a.charAt(off)=='N') 
//													off++;
//												transaction.betrag=new BigDecimalValue(
//														t19a.substring(off+3).replace(',','.'),
//														t19a.substring(off,off+3));
//												if (off>7)
//													transaction.betrag.setValue(transaction.betrag.getValue().negate());
//											} else if (t19a.startsWith(":ACRU")) {
//												int off=7;
//												if (t19a.charAt(off)=='N') 
//													off++;
//												transaction.stueckzinsen=new BigDecimalValue(
//														t19a.substring(off+3).replace(',','.'),
//														t19a.substring(off,off+3));
//												if (off>7)
//													transaction.stueckzinsen.setValue(transaction.stueckzinsen.getValue().negate());
//											} else {
//												System.out.println("Unbekannter 19A: " + t19a);
//											}
//										}
//
//										tagidx=0;
//										while (true) {
//											String t22f = Swift.getTagValue(onedetail, "22F", tagidx++);
//											if (t22f == null)
//												break;
//
//											if (t22f.startsWith(":TRAN")) {
//												if (t22f.endsWith("SETT")) {
//													transaction.transaction_indicator = Transaction.INDICATOR_SETTLEMENT_CLEARING;
//												} else if (t22f.endsWith("CORP")) {
//													transaction.transaction_indicator = Transaction.INDICATOR_CORPORATE_ACTION;
//												} else if (t22f.endsWith("BOLE")) {
//													transaction.transaction_indicator = Transaction.INDICATOR_LEIHE;
//												} else if (t22f.endsWith("COLL")) {
//													transaction.transaction_indicator = Transaction.INDICATOR_SICHERHEITEN;
//												} else {
//													System.out.println("Unbekannter 22F->TRAN: " + t22f);
//													transaction.transaction_indicator = -1;
//												}
//											} else if (t22f.startsWith(":CCPT")) {
//												if (t22f.endsWith("YCCP")) {
//													transaction.ccp_eligibility = true;
//												} else {
//													System.out.println("Unbekannter 22F->CCPT: " + t22f);
//												}
//											} else {
//												System.out.println("Unbekannter 22F: " + t22f);
//											}
//										}
//
//										tagidx=0;
//										while (true) {
//											String t22h = Swift.getTagValue(onedetail, "22H", tagidx++);
//											if (t22h == null)
//												break;
//
//											if (t22h.startsWith(":REDE")) {
//												if (t22h.endsWith("DELI")) {
//													transaction.richtung = Transaction.RICHTUNG_LIEFERUNG;
//												} else if (t22h.endsWith("RECE")) {
//													transaction.richtung = Transaction.RICHTUNG_ERHALT;
//												} else {
//													System.out.println("Unbekannter 22H->REDE: " + t22h);
//													transaction.richtung = -1;
//												}
//											} else if (t22h.startsWith(":PAYM")) {
//												if (t22h.endsWith("APMT")) {
//													transaction.bezahlung = Transaction.BEZAHLUNG_GEGEN_ZAHLUNG;
//												} else if (t22h.endsWith("FREE")) {
//													transaction.bezahlung = Transaction.BEZAHLUNG_FREI;
//												} else {
//													System.out.println("Unbekannter 22H->PAYM: " + t22h);
//													transaction.bezahlung = -1;
//												}
//											} else {
//												System.out.println("Unbekannter 22F: " + t22h);
//											}
//										}
//
//										tagidx=0;
//										while (true) {
//											String t98a = Swift.getTagValue(onedetail, "98A", tagidx++);
//											if (t98a == null)
//												break;
//
//											if (t98a.startsWith(":ESET")) {
//												String datum = t98a.substring(7);
//												transaction.datum = date_only_format.parse(datum);
//											} else if (t98a.startsWith(":SETT")) {
//												String datum = t98a.substring(7);
//												transaction.datum_valuta = date_only_format.parse(datum);
//											} else {
//												System.out.println("Unbekannter 98A: " + t98a);
//											}
//										}
//
//										String move = Swift.getTagValue(onedetail, "25D", 0);
//										if (move != null) 
//											if (move.startsWith(":MOVE")) {
//												if (move.endsWith("REVE"))
//													transaction.storno = true;
//											} else  {
//												System.out.println("Unbekannter 25D: " + move);
//											}
//
//										String freitext = Swift.getTagValue(onedetail, "70E", 0);
//										if (freitext != null) 
//											if (freitext.startsWith(":TRDE")) {
//												transaction.freitext_details = freitext.substring(7);
//											} else  {
//												System.out.println("Unbekannter 70E: " + freitext);
//											}
//									}
//								}
//
//								int party_start = onetransaction.indexOf(":16R:SETPRTY");
//								if (party_start >=0) {
//									int party_end = onetransaction.indexOf(":16S:SETPRTY", party_start);
//									if (party_end >= 0) {
//										String oneparty = onetransaction.substring(party_start, party_end+10);
//										String deag = Swift.getTagValue(oneparty, "95Q", 0);
//
//										if (deag != null) {
//											transaction.gegenpartei = deag.substring(7);
//										}
//									}
//								}
//
//								instrument.transactions.add(transaction);
//								trans_start = oneinstrument.indexOf(":16R:TRAN\r\n", trans_start);
//							}
//							entry.instruments.add(instrument);
//						}
//					} 
//					depot.addEntry(entry);
//					buffer.delete(0,onerecord.length());
//				} catch (Exception e) {
//					throw new HBCI_Exception("*** error while extracting data",e);
//				}
//			}
//
//			depot.rest = buffer.toString();                    
//		}          
//
//
//		public GVRWPDepotUms myExtract(String testdata) {
//			HBCIMsgStatus stat = new HBCIMsgStatus();
//			stat.getData().put("foo.data536", testdata);
//			extractResults(stat, "foo", 0);
//			return depot;
//		}
//	}
//
//	@Override
//	public boolean isSupported(Konto konto) throws ApplicationException, RemoteException {
//		String unterkontoExtract = "";
//		if (konto.getUnterkonto() != null && konto.getUnterkonto().toLowerCase().startsWith("depot")) {
//			unterkontoExtract = konto.getUnterkonto().toLowerCase().substring(5).replace(" ", ""); 
//		}
//
//		return 	"testing".equals(unterkontoExtract);
//	}
//
//	@Override
//	public void run(Konto konto) throws ApplicationException {
//		MyGVUms test = new MyGVUms();
//		String s;
//		try {
//			s = new String(Base64.decode(data2));
//		} catch (IOException e) {
//			throw new ApplicationException(e);
//		}
//		GVRWPDepotUms ret = test.myExtract(s);
//		System.out.println(ret.toString());
//		parseDepotUmsatz(ret, konto);
//	}
//
//
//	// Beispiel aus http://www.oekb.at/en/osn/DownloadCenter/capital-market/csd.austria/settlement/CSD.Austria-SWIFT-MT536-Specification.pdf
//	private String data = "OjE2UjpHRU5MDQo6MjhFOjEvT05MWQ0KOjIwQzo6U0VNRS8vVFIwOTM0MzA5NDU5MDAwMg0KOjIz\n" + 
//			"RzpORVdNDQo6OThDOjpQUkVQLy8yMDA5MTIwNjUxNDMzMg0KOjY5QTo6U1RBVC8vMjAwOTEyMDkv\n" + 
//			"MjAwOTEyMDkNCjoyMkY6OlNGUkUvL0FESE8NCjoyMkY6OkNPREUvL0NPTVANCjoyMkY6OlNUQkEv\n" + 
//			"L1NFVFQNCjo5N0E6OlNBRkUvLzk5OTkwMA0KOjE3Qjo6QUNUSS8vWQ0KOjE3Qjo6Q09OUy8vWQ0K\n" + 
//			"OjE2UzpHRU5MDQo6MTZSOlNVQlNBRkUNCjo5N0E6OlNBRkUvLzk5OTkwMA0KOjE3Qjo6QUNUSS8v\n" + 
//			"WQ0KOjE2UjpGSU4NCjozNUI6SVNJTiBBVDAwMDA2MTI2MDENCklOVEVSQ0VMTCBBRyBBS1RJRU4g\n" + 
//			"T0hORSBORU5OV0VSVA0KOjkzQjo6RklPUC8vVU5JVC8yNDksDQo6OTNCOjpGSUNMLy9VTklULzAs\n" + 
//			"DQo6MTZSOlRSQU4NCjoxNlI6TElOSw0KOjIwQzo6UkVMQS8vMjc4NTY5OTUxDQo6MTZTOkxJTksN\n" + 
//			"CjoxNlI6VFJBTlNERVQNCjozNkI6OlBTVEEvL1VOSVQvMjUwLA0KOjE5QTo6UFNUQS8vNjEzNCw4\n" + 
//			"Nw0KOjIyRjo6VFJBTi8vU0VUVA0KOjIySDo6UkVERS8vREVMSQ0KOjIySDo6UEFZTS8vQVBNVA0K\n" + 
//			"Ojk4QTo6RVNFVC8vMjAwOTEyMDkNCjoxNlI6U0VUUFJUWQ0KOjk1UTo6UkVBRy8vMjI1NjAwDQo6\n" + 
//			"MTZTOlNFVFBSVFkNCjoxNlM6VFJBTlNERVQNCjoxNlM6VFJBTg0KOjE2UjpUUkFODQo6MTZSOkxJ\n" + 
//			"TksNCjoyMEM6OlJFTEEvL05PTlJFRg0KOjE2UzpMSU5LDQo6MTZSOlRSQU5TREVUDQo6MzZCOjpQ\n" + 
//			"U1RBLy9VTklULzEwMDAsDQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJIOjpSRURFLy9SRUNFDQo6MjJI\n" + 
//			"OjpQQVlNLy9GUkVFDQo6OThBOjpFU0VULy8yMDA5MTIwOQ0KOjE2UjpTRVRQUlRZDQo6OTVROjpE\n" + 
//			"RUFHLy85OTk5MTANCjoxNlM6U0VUUFJUWQ0KOjE2UzpUUkFOU0RFVA0KOjE2UzpUUkFODQo6MTZS\n" + 
//			"OlRSQU4NCjoxNlI6TElOSw0KOjIwQzo6UkVMQS8vMDlKODcwNzYNCjoxNlM6TElOSw0KOjE2UjpU\n" + 
//			"UkFOU0RFVA0KOjM2Qjo6UFNUQS8vVU5JVC85OTksDQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJIOjpS\n" + 
//			"RURFLy9ERUxJDQo6MjJIOjpQQVlNLy9GUkVFDQo6MjJGOjpDQ1BULy9ZQ0NQDQo6OThBOjpFU0VU\n" + 
//			"Ly8yMDA5MTIwOQ0KOjE2UjpTRVRQUlRZDQo6OTVROjpSRUFHLy8yNDAwMDANCjoxNlM6U0VUUFJU\n" + 
//			"WQ0KOjE2UzpUUkFOU0RFVA0KOjE2UzpUUkFODQo6MTZTOkZJTg0KOjE2UjpGSU4NCjozNUI6SVNJ\n" + 
//			"TiBBVDAwMDA2MDYzMDYNClJBSUZGRUlTRU4gSU5URVJOLkJBTkstSE9MRC5BRyBBS1RJDQpFTiBP\n" + 
//			"SE5FIE5FTk5XRVINCjo5M0I6OkZJT1AvL1VOSVQvMCwNCjo5M0I6OkZJQ0wvL1VOSVQvMCwNCjox\n" + 
//			"NlI6VFJBTg0KOjE2UjpMSU5LDQo6MjBDOjpSRUxBLy8rMDlKODcwNzUNCjoxNlM6TElOSw0KOjE2\n" + 
//			"UjpUUkFOU0RFVA0KOjM2Qjo6UFNUQS8vVU5JVC80MTYsDQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJI\n" + 
//			"OjpSRURFLy9SRUNFDQo6MjJIOjpQQVlNLy9GUkVFDQo6MjJGOjpDQ1BULy9ZQ0NQDQo6OThBOjpF\n" + 
//			"U0VULy8yMDA5MTIwOQ0KOjE2UjpTRVRQUlRZDQo6OTVROjpERUFHLy8yNDAwMDANCjoxNlM6U0VU\n" + 
//			"UFJUWQ0KOjE2UzpUUkFOU0RFVA0KOjE2UzpUUkFODQo6MTZSOlRSQU4NCjoxNlI6TElOSw0KOjIw\n" + 
//			"Qzo6UkVMQS8vODgyNTczMzA1DQo6MTZTOkxJTksNCjoxNlI6VFJBTlNERVQNCjozNkI6OlBTVEEv\n" + 
//			"L1VOSVQvNDE2LA0KOjE5QTo6UFNUQS8vRVVSMTgxNTQsODINCjoyMkY6OlRSQU4vL1NFVFQNCjoy\n" + 
//			"Mkg6OlJFREUvL0RFTEkNCjoyMkg6OlBBWU0vL0FQTVQNCjo5OEE6OkVTRVQvLzIwMDkxMjA5DQo6\n" + 
//			"MTZSOlNFVFBSVFkNCjo5NVE6OlJFQUcvLzIyNzMwMA0KOjE2UzpTRVRQUlRZDQo6MTZTOlRSQU5T\n" + 
//			"REVUDQo6MTZTOlRSQU4NCjoxNlM6RklODQo6MTZSOkZJTg0KOjM1QjpJU0lOIEFUMDAwMDkwODUw\n" + 
//			"NA0KVklFTk5BIElOU1VSQU5DRSBHUk9VUCBTVEFNTUFLVElFTg0KT0hORSBORU5OV0VSVA0KOjkz\n" + 
//			"Qjo6RklPUC8vVU5JVC8wLA0KOjkzQjo6RklDTC8vVU5JVC8wLA0KOjE2UjpUUkFODQo6MTZSOkxJ\n" + 
//			"TksNCjoyMEM6OlJFTEEvLyswOUo4NzA4NQ0KOjE2UzpMSU5LDQo6MTZSOlRSQU5TREVUDQo6MzZC\n" + 
//			"OjpQU1RBLy9VTklULzEwMTgsDQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJIOjpSRURFLy9SRUNFDQo6\n" + 
//			"MjJIOjpQQVlNLy9GUkVFDQo6MjJGOjpDQ1BULy9ZQ0NQDQo6OThBOjpFU0VULy8yMDA5MTIwOQ0K\n" + 
//			"OjE2UjpTRVRQUlRZDQo6OTVROjpERUFHLy8yNDAwMDANCjoxNlM6U0VUUFJUWQ0KOjE2UzpUUkFO\n" + 
//			"U0RFVA0KOjE2UzpUUkFODQo6MTZSOlRSQU4NCjoxNlI6TElOSw0KOjIwQzo6UkVMQS8vMjU3NjYx\n" + 
//			"MA0KOjE2UzpMSU5LDQo6MTZSOlRSQU5TREVUDQo6MzZCOjpQU1RBLy9VTklULzQzMywNCjoxOUE6\n" + 
//			"OlBTVEEvL0VVUjE1ODAxLDI4DQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJIOjpSRURFLy9ERUxJDQo6\n" + 
//			"MjJIOjpQQVlNLy9BUE1UDQo6OThBOjpFU0VULy8yMDA5MTIwOQ0KOjE2UjpTRVRQUlRZDQo6OTVR\n" + 
//			"OjpSRUFHLy8yNDM5MDANCjoxNlM6U0VUUFJUWQ0KOjE2UzpUUkFOU0RFVA0KOjE2UzpUUkFODQo6\n" + 
//			"MTZSOlRSQU4NCjoxNlI6TElOSw0KOjIwQzo6UkVMQS8vMTIzNDU2DQo6MTZTOkxJTksNCjoxNlI6\n" + 
//			"VFJBTlNERVQNCjozNkI6OlBTVEEvL1VOSVQvNTg1LA0KOjIyRjo6VFJBTi8vU0VUVA0KOjIySDo6\n" + 
//			"UkVERS8vREVMSQ0KOjIySDo6UEFZTS8vRlJFRQ0KOjk4QTo6RVNFVC8vMjAwOTEyMDkNCjoxNlI6\n" + 
//			"U0VUUFJUWQ0KOjk1UTo6UkVBRy8vMjIyMTAwDQo6MTZTOlNFVFBSVFkNCjoxNlM6VFJBTlNERVQN\n" + 
//			"CjoxNlM6VFJBTg0KOjE2UzpGSU4NCjoxNlM6U1VCU0FGRQ0KOjE2UjpTVUJTQUZFDQo6OTdBOjpT\n" + 
//			"QUZFLy85OTk5OTkNCjoxN0I6OkFDVEkvL04NCjoxNlM6U1VCU0FGRQ0KOjE2UjpTVUJTQUZFDQo6\n" + 
//			"OTdBOjpTQUZFLy85OTk5NTENCjoxN0I6OkFDVEkvL04NCjoxNlM6U1VCU0FGRQ0KOjE2UjpTVUJT\n" + 
//			"QUZFDQo6OTdBOjpTQUZFLy85OTk5NzQNCjoxN0I6OkFDVEkvL04NCjoxNlM6U1VCU0FGRQ0KOjE2\n" + 
//			"UjpTVUJTQUZFDQo6OTdBOjpTQUZFLy85OTk5OTANCjoxN0I6OkFDVEkvL04NCjoxNlM6U1VCU0FG\n" + 
//			"RQ0KOjE2UjpTVUJTQUZFDQo6OTdBOjpTQUZFLy85OTk5MTANCjoxN0I6OkFDVEkvL1kNCjoxNlI6\n" + 
//			"RklODQo6MzVCOklTSU4gQVQwMDAwNjEyNjAxDQpJTlRFUkNFTEwgQUcgQUtUSUVOIE9ITkUgTkVO\n" + 
//			"TldFUlQNCjo5M0I6OkZJT1AvL1VOSVQvMTAwMCwNCjo5M0I6OkZJQ0wvL1VOSVQvMCwNCjoxNlI6\n" + 
//			"VFJBTg0KOjE2UjpMSU5LDQo6MjBDOjpSRUxBLy9DQVBNLUlDTEwgOS8xMg0KOjE2UzpMSU5LDQo6\n" + 
//			"MTZSOlRSQU5TREVUDQo6MzZCOjpQU1RBLy9VTklULzEwMDAsDQo6MjJGOjpUUkFOLy9TRVRUDQo6\n" + 
//			"MjJIOjpSRURFLy9ERUxJDQo6MjJIOjpQQVlNLy9GUkVFDQo6OThBOjpFU0VULy8yMDA5MTIwOQ0K\n" + 
//			"OjE2UjpTRVRQUlRZDQo6OTVROjpSRUFHLy85OTk5MDANCjoxNlM6U0VUUFJUWQ0KOjE2UzpUUkFO\n" + 
//			"U0RFVA0KOjE2UzpUUkFODQo6MTZTOkZJTg0KOjE2UzpTVUJTQUZFDQoNCg==";
//
//	// Beispiel aus http://www.hbci-zka.de/dokumente/spezifikation_deutsch/fintsv3/FinTS_3.0_Messages_Finanzdatenformate_2010-08-06_final_version.pdf
//	private String data2 = "OjE2UjpHRU5MDQo6MjhFOjEvT05MWQ0KOjEzQTo6U1RBVC8vMDA1DQo6MjBDOjpTRU1FLy9OT05S\n" + 
//			"RUYNCjoyM0c6TkVXTQ0KOjk4QTo6UFJFUC8vMTk5OTA1MzANCjo2OUE6OlNUQVQvLzE5OTkwNTAx\n" + 
//			"LzE5OTkwNTI5DQo6OTdBOjpTQUZFLy8xMDAyMDAzMC8xMjM0NTY3DQo6MTdCOjpBQ1RJLy9ZDQo6\n" + 
//			"MTZTOkdFTkwNCjoxNlI6RklODQo6MzVCOklTSU4gREUwMTIzNDU2Nzg5DQovREUvMTIzNDU2DQpN\n" + 
//			"dXN0ZXJtYW5uIEFHLCBTdGFtbWFrdGllbg0KOjkwQjo6TVJLVC8vQUNUVS9FVVI1Miw3DQo6OTRC\n" + 
//			"OjpQUklDLy9MTUFSL1hGUkENCjo5OEE6OlBSSUMvLzE5OTkwNTE1DQo6OTNCOjpGSU9QLy9VTklU\n" + 
//			"LzIwMCwNCjo5M0I6OkZJQ0wvL1VOSVQvMzAwLA0KOjE2UjpUUkFODQo6MTZSOkxJTksNCjoyMEM6\n" + 
//			"OlJFTEEvL05PTlJFRg0KOjE2UzpMSU5LDQo6MTZSOlRSQU5TREVUDQo6MzZCOjpQU1RBLy9VTklU\n" + 
//			"LzEwMCwNCjoxOUE6OlBTVEEvL05FVVI1MjcwLA0KOjIyRjo6VFJBTi8vU0VUVA0KOjIySDo6UkVE\n" + 
//			"RS8vUkVDRQ0KOjIySDo6UEFZTS8vRlJFRQ0KOjk4QTo6RVNFVC8vMTk5OTA1MTUNCjo5OEE6OlNF\n" + 
//			"VFQvLzE5OTkwNTE3DQo6MTZTOlRSQU5TREVUDQo6MTZTOlRSQU4NCjoxNlM6RklODQo6MTZSOkZJ\n" + 
//			"Tg0KOjM1QjpJU0lOIERFMDEyMzQ1Njc4OQ0KL0RFLzEyMzQ1Ng0KTXVzdGVybWFubiBBRywgU3Rh\n" + 
//			"bW1ha3RpZW4NCjo5MEI6Ok1SS1QvL0FDVFUvRVVSNjEsOQ0KOjk0Qjo6UFJJQy8vTE1BUi9YRlJB\n" + 
//			"DQo6OThBOjpQUklDLy8xOTk5MDUyOA0KOjkzQjo6RklPUC8vVU5JVC8zMDAsDQo6OTNCOjpGSUNM\n" + 
//			"Ly9VTklULzIzMCwNCjoxNlI6VFJBTg0KOjE2UjpMSU5LDQo6MjBDOjpSRUxBLy9OT05SRUYNCjox\n" + 
//			"NlM6TElOSw0KOjE2UjpUUkFOU0RFVA0KOjM2Qjo6UFNUQS8vVU5JVC83MCwNCjoxOUE6OlBTVEEv\n" + 
//			"L0VVUjQzMzMsDQo6MjJGOjpUUkFOLy9TRVRUDQo6MjJIOjpSRURFLy9ERUxJDQo6MjJIOjpQQVlN\n" + 
//			"Ly9GUkVFDQo6OThBOjpFU0VULy8xOTk5MDUyOA0KOjk4QTo6U0VUVC8vMTk5OTA1MzANCjoxNlM6\n" + 
//			"VFJBTlNERVQNCjoxNlM6VFJBTg0KOjE2UzpGSU4NCjoxNlI6RklODQo6MzVCOi9ERS85ODc2NTQN\n" + 
//			"CkRhaW1sZXJDaHJ5c2xlciBMdXguIEZpbi4NCjE5OTkgKDIwMDIpDQo6OTBCOjpNUktULy9QUkNU\n" + 
//			"LzEwNSwNCjo5NEI6OlBSSUMvL0xNQVIvWExVWA0KOjk4QTo6UFJJQy8vMTk5OTA1MjENCjo5M0I6\n" + 
//			"OkZJT1AvL0ZBTVQvNTAwMCwNCjoxNlI6VFJBTg0KOjE2UjpMSU5LDQo6MjBDOjpSRUxBLy9OT05S\n" + 
//			"RUYNCjoxNlM6TElOSw0KOjE2UjpUUkFOU0RFVA0KOjM2Qjo6UFNUQS8vRkFNVC81MDAwLA0KOjk5\n" + 
//			"QTo6REFBQy8vMDAzDQo6MTlBOjpQU1RBLy9DQUQ1MjUwLA0KOjE5QTo6QUNSVS8vQ0FEMiw3MQ0K\n" + 
//			"OjIyRjo6VFJBTi8vU0VUVA0KOjIySDo6UkVERS8vREVMSQ0KOjIySDo6UEFZTS8vRlJFRQ0KOjk4\n" + 
//			"QTo6RVNFVC8vMTk5OTA1MjENCjo5OEE6OlNFVFQvLzE5OTkwNTI2DQo6MTZTOlRSQU5TREVUDQo6\n" + 
//			"MTZTOlRSQU4NCjoxNlM6RklODQo=\n";
//
//
//	@Override
//	public String getName() {
//		return "Muster";
//	}
//}
