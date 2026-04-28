%====================================================================================
% firefly description   
%====================================================================================
dispatch( cellstate, cellstate(X,Y,COLOR) ). %set color of cell X,Y
%====================================================================================
context(ctxfirefly, "localhost",  "TCP", "8040").
 qactor( firefly1, ctxfirefly, "it.unibo.firefly1.Firefly1").
 static(firefly1).
  qactor( griddisplaymock, ctxfirefly, "it.unibo.griddisplaymock.Griddisplaymock").
 static(griddisplaymock).
