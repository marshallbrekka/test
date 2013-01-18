(ns borg.transport.interface)

(defprotocol ITransport
  (start-server [_ handler-executer port])
  (stop-server [_ server])
  (create-client [_ host port])
  (close-client [_ client])
  (send-command [_ client handler-spec]))
