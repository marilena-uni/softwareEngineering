%====================================================================================
% fireflysinch description   
%====================================================================================
dispatch( cellstate, cellstate(X,Y,COLOR) ). %commute cell state
event( timer, timer(T) ).
%====================================================================================
context(ctxfirefly, "localhost",  "TCP", "8040").
context(ctxgrid, "127.0.0.1",  "TCP", "8050").
 qactor( griddisplay, ctxgrid, "external").
  qactor( cordinator, ctxfirefly, "it.unibo.cordinator.Cordinator").
 static(cordinator).
  qactor( firefly1, ctxfirefly, "it.unibo.firefly1.Firefly1").
 static(firefly1).
  qactor( firefly2, ctxfirefly, "it.unibo.firefly2.Firefly2").
 static(firefly2).
  qactor( firefly3, ctxfirefly, "it.unibo.firefly3.Firefly3").
 static(firefly3).
