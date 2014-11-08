package com.katlex.expenses

import unfiltered.jetty.Http

object Server {

  def main(args: Array[String]) {

    data.createSchemaOpt()

    Http(8080).context("/assets") {
      _.resources(Page.assets)
    }.filter(unfiltered.filter.Planify {
      Page.intent orElse Api.intent
    }).run()
  }
}
