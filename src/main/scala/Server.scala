
import unfiltered.jetty.Http

import unfiltered.request.Path

object Server {
  def main(args: Array[String]) {
    Http(8080).context("/assets") {
      _.resources(Page.assets)
    }.filter(unfiltered.filter.Planify {
      case Path("/") => Page("Hello")(Nil)(Nil)(
        <h1>
        <div>Hello world</div>
        </h1>)
    }).run
  }
}
