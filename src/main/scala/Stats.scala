class Stats {
  var messages: Int = 0
  var allocated: Int = 0
  var checks: Int = 0
  var touches: Int = 0
  var misses: Int = 0
  var errors: Int = 0

  // added these to test command
  var bought: Int = 0;
  var emptyKiosk: Int = 0;
  var moreTickets: Int = 0;
  var chunkConcurrencyError:Int = 0;
  var soldOut: Int = 0;


  def += (right: Stats): Stats = {
    messages += right.messages
    allocated += right.allocated
    checks += right.checks
    touches += right.touches
    misses += right.misses
    errors += right.errors

    bought += right.bought
    emptyKiosk += right.emptyKiosk
    moreTickets += right.moreTickets
    soldOut += right.soldOut
    chunkConcurrencyError += right.chunkConcurrencyError
    this
  }

  override def toString(): String = {
    s"Stats TicketRequests=$messages TicketsBought=$bought EmptyKiosk=$emptyKiosk MoreTicketsReq=$moreTickets ChunkConcurrencyError=$chunkConcurrencyError SoldOutMsg=$soldOut"
  }
}
