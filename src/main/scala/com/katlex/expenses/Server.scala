package com.katlex.expenses

import unfiltered.jetty.Http

import unfiltered.request.Path

object Server {

  def main(args: Array[String]) {

    data.createSchemaOpt()
    println(data.user("alun@katlex.com"))
    println(data.user("alun@example.com"))

    Http(8080).context("/assets") {
      _.resources(Page.assets)
    }.filter(unfiltered.filter.Planify {
      case Path("/") => Page("Expenses tracker")
    }).run
  }
}
