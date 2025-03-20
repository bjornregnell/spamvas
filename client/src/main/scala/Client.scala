package snackomaten 

class Client(val userId: String, val host: String = "bjornix.cs.lth.se", val port: Int = 8090):

  val retryReconnectAfterMillis = 5000

  val isWatching = Concurrent.ThreadSafe.MutableFlag(true)

  val isShowStackTrace = false

  @volatile private var connectionOpt: Option[Network.Connection] = None

  def isActive = connectionOpt.isDefined && connectionOpt.get.isActive

  def closeConnection(): Unit = for c <- connectionOpt do 
    Terminal.putRed(s"Closing ${if !isActive then "inactive " else ""}connection $c")
    try c.close() catch case e: Throwable => Terminal.showError(e, showStackTrace = isShowStackTrace)
    connectionOpt = None
    Terminal.putRed("No connection.")

  final def retryConnectIfNoActiveConnection(): Unit = this.synchronized:
    var continue = false
    while 
      continue = false
      if !isActive then closeConnection()
      if !connectionOpt.isDefined then
        try 
          connectionOpt = Some(Network.Connection.fromSocket(java.net.Socket(host, port)))
        catch case e: Throwable => 
          Terminal.showError(e, showStackTrace = isShowStackTrace)
          Terminal.alert("Server seems currently unavailable...")
          connectionOpt = None
          Terminal.putYellow(s"Attempting to reconnect after ${(retryReconnectAfterMillis/1000.0).round} s ...")
          Thread.sleep(retryReconnectAfterMillis)
          continue = true
      continue
    do ()


  def send(msg: String): Unit = 
    retryConnectIfNoActiveConnection()
    Terminal.putGreen(s"Sending '$msg' via $connectionOpt: isActive=$isActive")
    try connectionOpt.get.write(s"userId=$userId;msg=$msg")
    catch case e: Throwable => 
      Terminal.showError(e, showStackTrace = isShowStackTrace)
      closeConnection()
      send(msg)

  def receive(): String =
    retryConnectIfNoActiveConnection()
    try connectionOpt.get.read()
    catch case e: Throwable => 
      Terminal.showError(e, showStackTrace = isShowStackTrace)
      for c <- connectionOpt do try c.close() catch case e: Throwable => Terminal.showError(e)
      connectionOpt = None
      receive()

  def spawnReceiveLoop() = 
    Concurrent.Run:
      Terminal.putRed("spawnReceiveLoop() started in new thread!")
      Terminal.putGreen(s"Listening for messages until !stop in ${Thread.currentThread()}")
      while isWatching.isTrue do
        val msg: String = receive()
        val i = msg.indexOf("msg=")
        if i < 1 then Terminal.putRed(msg) else
          Terminal.putGreen(msg.substring(0, i - 1) + " sent this message:")
          Terminal.put(msg.substring(i + 4))
      end while
      Terminal.putGreen("spawnReceiveLoop() thread done.")

  def helpText = """
    Type just <ENTER> to toggle watch mode.
    Type some text <ENTER> to spam all connected clients with a message.
  """

  def commandLoop(): Unit = 
    var continue = true
    while continue do
      val info = s"type command or message from $userId> "

      if isWatching.isTrue then Terminal.putBlue(info) else Terminal.putRed(info + " watch mode off, buffering messages")
      
      val cmd = Terminal.get()
      if cmd == Terminal.CtrlD then continue = false 
      else if cmd.isEmpty then 
        isWatching.toggle() 
        Terminal.putGreen(s"Toggled watch mode: isWatching=${isWatching.isTrue}")
        Terminal.putYellow(helpText)
      else 
        send(cmd)
    end while
    for c <- connectionOpt do c.close()

  def start(): Unit = 
    Terminal.putYellow(s"Connecting to snackomaten.Server host=$host port=$port")
    retryConnectIfNoActiveConnection()
    Terminal.putGreen("Connected!")
    Terminal.putYellow(helpText)
    spawnReceiveLoop()
    commandLoop()
  
end Client