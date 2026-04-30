%====================================================================================
% fireflysystem description   
%====================================================================================
dispatch( cellstate, cellstate(X,Y,COLOR) ).
event( timer, timer(T) ).
%====================================================================================
context(ctxfirefly, "localhost",  "TCP", "8040").
context(ctxgrid, "127.0.0.1",  "TCP", "8050").
 qactor( griddisplay, ctxgrid, "external").
  qactor( cordinator, ctxfirefly, "it.unibo.cordinator.Cordinator").
 static(cordinator).
  qactor( firefly1, ctxfirefly, "it.unibo.firefly1.Firefly1").
 static(firefly1).
