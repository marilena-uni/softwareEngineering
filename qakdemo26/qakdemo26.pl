%====================================================================================
% qakdemo26 description   
%====================================================================================
request( distance, distance(D) ).
reply( distanceack, ack(D) ).  %%for distance
request( r2, r2(X) ).
reply( rr2, rr2(X) ).  %%for r2
%====================================================================================
context(ctxqakdemo26, "localhost",  "TCP", "8045").
 qactor( creator, ctxqakdemo26, "it.unibo.creator.Creator").
 static(creator).
  qactor( producer, ctxqakdemo26, "it.unibo.producer.Producer").
dynamic(producer). %%Oct2023 
  qactor( consumer, ctxqakdemo26, "it.unibo.consumer.Consumer").
 static(consumer).
  qactor( consumerhelper, ctxqakdemo26, "it.unibo.consumerhelper.Consumerhelper").
dynamic(consumerhelper). %%Oct2023 
