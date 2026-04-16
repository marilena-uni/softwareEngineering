package conway.io;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import main.java.conway.domain.GameController;
import main.java.conway.domain.IGrid;
import main.java.conway.domain.IOutDev;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ApplMessage;

public class OutInWs implements IOutDev{
	
	private static AtomicInteger pageCounter = new AtomicInteger(0);
	private WsMessageContext pageCtx, lifeCtrlCtx ;
	private String name;
	private String firstCaller        = null;
	private GameController controller;
	//private WsConnectContext ownerctx = null;
	protected Vector<WsConnectContext> allConns = new Vector<WsConnectContext>();

	public OutInWs(String name) {
		this.name = name;
        var app = Javalin.create(config -> {
        	// Configurazione globale del timeout per le connessioni (dalla versione 6.x in avanti)
            //config.http.asyncTimeout = 300000L; // 5 minuti in millisecondi
        	config.jetty.modifyWebSocketServletFactory(factory -> {
                // Imposta il timeout (ad esempio 5 minuti)
                factory.setIdleTimeout(Duration.ofMinutes(30));
            });
        	config.staticFiles.add(staticFiles -> {
				staticFiles.directory = "/page";
				staticFiles.location = Location.CLASSPATH; // Cerca dentro il JAR/Classpath
				/*
				 * i file sono "impacchettati" con il codice, non cercati sul disco rigido esterno.
				 */
		    });
		}).start(8080);
 
/*
 * --------------------------------------------
 * Parte HTTP        
 * --------------------------------------------
 */
        app.get("/", ctx -> {
    		//Path path = Path.of("./src/main/resources/page/ConwayInOutPage.html");    		    
        	/*
        	 * Java cercherà il file all'interno del Classpath 
        	 * (dentro il JAR o nelle cartelle dei sorgenti di Eclipse), 
        	 * rendendo il codice universale
         	 */
        	//var inputStream = getClass().getResourceAsStream("/page/ConwayInOutPage.html");  
        	var inputStream = getClass().getResourceAsStream("/page/LifeIInOutCanvas.html");     
        	if (inputStream != null) {
        		// Trasformiamo l'inputStream in stringa (o lo mandiamo come stream)
        	    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        	    ctx.html(content);
        	} else {
		        ctx.status(404).result("File non trovato nel file system");
		    }
		    //ctx.result("Hello from Java!"));  //la forma più semplice di risposta
        }); 
        
//        app.get("/greet/{name}", ctx -> {
//            String pname = ctx.pathParam("name");
//            ctx.result("Hello, " + pname + "!");
//        }); //http://localhost:8080/greet/Alice
//        
//        app.get("/api/users", ctx -> {
//            Map<String, Object> user = Map.of("id", 1, "name", "Bob");
//            ctx.json(user); // Auto-converts to JSON
//        });
        
        /*
         * Javalin v5+: Si passa solo la "promessa" (il Supplier del Future). 
         * Javalin è diventato più intelligente: se il Future restituisce una Stringa, 
         * lui fa ctx.result(stringa). Se restituisce un oggetto, lui fa ctx.json(oggetto).
         * 
         */
//        app.get("/async", ctx -> {
//        	ctx.future(() -> {
//	        	// Creiamo il future
//	            CompletableFuture<String> future = new CompletableFuture<>();
//	            
//	            // Eseguiamo il lavoro in un altro thread
//	            new Thread(() -> { 
//	                try {
//	                    Thread.sleep(2000); // Simulazione calcolo pesante
//	                    future.complete(name + " | Risultato calcolato asincronamente");
//	                } catch (Exception e) {
//	                    future.completeExceptionally(e);
//	                }
//	            });
//	            
//	            return future; // Restituiamo il future a Javalin
//        	});
//        });
//        
//        app.get("/async1", ctx -> {
//            ctx.future(() -> CompletableFuture.supplyAsync(() -> {
//                // Simuliamo l'operazione lenta
//                try {
//                    Thread.sleep(2000); 
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return name + " | Risultato calcolato con supplyAsync";
//            }));
//        });
/*
 * --------------------------------------------
 * Parte Websocket
 * --------------------------------------------
 */      
        app.ws("/eval", ws -> {
            ws.onConnect(
		            ctx -> { //myctx=ctx; 
    	        int idAssegnato = pageCounter.incrementAndGet();
    	        String callerName = "caller"+idAssegnato;
     	        sendsafe(ctx, "ID:" + callerName);
     			if( firstCaller == null ) {
     				firstCaller = callerName;
     				//ownerctx    = ctx;
     			}
     			CommUtils.outmagenta("connected ..." + callerName);
     			allConns.add(ctx);
     			 
        // Ogni 20 secondi invia un segnale per "svegliare" i proxy
//    	heartbeatTask = executor.scheduleAtFixedRate(
//            () -> { if(ctx.session.isOpen()) sendsafe(ctx,"PING");}, //lambda // CommUtils.outcyan("PING");
//                    20,  //QUANTO ASPETTARE LA PRIMA VOLTA. Se 0, il primo PING parte istantaneamente (inutile)
//                    20,  //OGNI QUANTO RIPETERE
//                    TimeUnit.SECONDS
//            );
		            });
            
            ws.onClose(ctx -> {
                allConns.remove(ctx);
                if(ctx.equals(pageCtx)) pageCtx = null;
                CommUtils.outred("Client disconnected. Remaining: " + allConns.size());
            } );
             
            ws.onMessage(ctx -> {
                String message = ctx.message();     
                try {
                	//La pagina e il mondo esterno comuicano col server con IApplMessage
                	IApplMessage m = new ApplMessage(message);
                    //CommUtils.outblue(name + " |  eval:" + m.msgContent() );
                	if( m.msgContent().startsWith("[[")) {
                		pageCtx.send( m.msgContent()); 
                		return;
                	}
                    CommUtils.outcyan(name + " |  eval receives:" + message + " pageCtx=" + (pageCtx!=null) + " allConns:" +allConns.size());
                    if(  m.msgSender().equals("unknown") && m.msgContent().contains("canvasready") && pageCtx==null) { 
                    	pageCtx = ctx;  //memorizzo connessione pagina
                    	CommUtils.outmagenta(name + " |  memorizzo pageCtx:" + pageCtx);
                    }else if( m.msgSender().equals("lifectrl") && m.msgId().contains("setcontroller")) { 
                    	lifeCtrlCtx = ctx; //memorizzo connessione controller
                    	CommUtils.outmagenta(name + " |  memorizzo lifeCtrlCtx:" + lifeCtrlCtx );
            			sendsafe( lifeCtrlCtx, "msg( eval, dispatch, caller1, lifectrl, clear, 0 )" );
                     }else if( m.msgReceiver().equals(name) && m.msgContent().contains("cell(")) {                   
                    	if (pageCtx != null) {
                        	//Funziona se ci sono 3 arg - es. cell(5,6,1)
                    		decodeAndForwardCell(m.msgContent());
                    		pageCtx.send( m.msgContent()); 
                     	}
                    	if( m.isRequest() ) {  //m viene da fuori, non dalla pagina
                    		IApplMessage reply = CommUtils.buildReply(name,"replyTo_"+m.msgId(),"done",m.msgSender()); 
                    		ctx.send(reply.toString());
                      	}else if( m.msgSender().equals(firstCaller) ){
                     		//ctx.send("updateCellColor sent from page");
                     		CommUtils.outmagenta("lifeCtrlCtx send " + m);
                    		if( lifeCtrlCtx != null ) 
                    			sendsafe( lifeCtrlCtx, m.toString() );
                     	}
                    	
                    }else if( m.msgReceiver().equals("lifectrl") ) {  
                     	if( m.msgSender().equals("unknown") ){
                    		//Nuova paginna collegata
                    		return;
                    	}
                    	if( m.msgSender().equals(firstCaller) ){
                       		CommUtils.outblue(name + " sending to lifeCtrlCtx: " + m);
                    		if( lifeCtrlCtx != null ) 
                    			sendsafe( lifeCtrlCtx, m.toString() );
                    		//msg( eval, dispatch, caller1, lifectrl, clear, 0 )
                     	}else {
                     		CommUtils.outred("lifeCtrlCtx send to page ???" + m.msgContent() );
//                    		if( pageCtx != null ) 
//                    			sendsafe( pageCtx,  m.msgContent() );                   		
                     	}
                    }
                 else if (m.msgContent().equals("start")) {
                    if(controller != null) controller.onStart(); 
                } else if (m.msgContent().equals("stop")) {
                    if(controller != null) controller.onStop();
                } else if (m.msgContent().equals("clear")) {
                    if(controller != null) controller.onClear();
                }
//                    CommUtils.outcyan(name + " |  allConns:" +allConns.size());
//                    allConns.forEach( (conn) ->	conn.send(m.msgContent()) );
                }catch(Exception e) {
                	CommUtils.outred(name + " |  not a IApplMessage:" + message);
//                	allConns.forEach( (conn) ->	conn.send(message) );
                }               
            });
        });        
	}
	
    protected void sendsafe(WsContext ctx, String msg) {
    	synchronized (ctx.session) {  
            if (ctx.session.isOpen()) {
                ctx.send(msg);
            }
        }
    }
	
 // funzione di appoggio per estrarre riga e colonna della cella e andare a cambiarle stato
 	private void decodeAndForwardCell(String mess)
 	{
 		try {
 		String data = mess.replace("cell(", "").replace(")", "");
 		String[] part = data.split(",");
 		
 		int x = Integer.parseInt(part[0].trim());
 		int y = Integer.parseInt(part[1].trim());
 		
 		controller.switchCellState(x, y);}
 		
 		catch (Exception e) {
 	        CommUtils.outred("Errore decodifica cella: " + e.getMessage());
 	    }
 	}

    
	
	public static void main(String[] args) {
		var resource = IoJavalin.class.getResource("/pages");
		CommUtils.outgreen("DEBUG: La cartella /page si trova in: " + resource);
		new IoJavalin("guiserver");
	}
	
	public void setController(GameController controller) {
		this.controller = controller;
		}

	@Override
	public void display(String msg) {
		
		if ( lifeCtrlCtx != null && this.lifeCtrlCtx.session.isOpen()) {
			lifeCtrlCtx.send(msg);
	    }
		this.allConns.forEach(session -> {
	        if (session.session.isOpen()) {
	            session.send(msg);
	        }
	    });
	}

	@Override
	public void displayCell(IGrid grid, int x, int y) {
		
		int state = grid.getCellValue(x, y) ? 1 : 0;
		String mess = "cell(" + x + "," + y + "," + state + ")";


		display(mess);
		
	}

	@Override
	public void close() {
		allConns.forEach(ctx -> ctx.session.close());
	    allConns.clear();
	    CommUtils.outred("IoJavalin | Risorsa chiusa.");
		
	}

	@Override
	public void displayGrid(IGrid grid) {
		
		for(int i=0; i<grid.getRowsNum(); i++)
		{
			for(int j=0; j<grid.getColsNum(); j++)
			{
				this.displayCell(grid, i, j);
			}
		}
	}

}
