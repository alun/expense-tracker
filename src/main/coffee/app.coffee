body = document.body.cloneNode(true)

@runApp = ->
  log = (args...) -> console?.log?(args...)

  app = angular.module 'expenses', ['ngResource']

  serialize = (form) ->
    i = 0
    res = {}
    while field = form[i++]
      v = null
      if ['text', 'password'].indexOf(field.type) != -1
        v = field.value
      else if field.type == 'checkbox'
        v = field.checked
      res[field.name] = v if v?
    res

  app.factory "userApi", ['$resource', (resource) ->
    resource "/api/users/:id/:verb",
        id: '@username'
      ,
        login:
          method: 'POST'
          params: verb: 'login'
  ]

  app.directive 'loginForm', ['userApi', (api) ->
    (scope, element, attrs) ->
      form = element[0]
      element.on 'submit', ->
        api.login (serialize form)
  ]

  restartAngular = ->
    clearChildren = (node) ->
      while node.children.length > 0
        node.removeChild node.firstChild

    clearChildren document.body
    document.body = body.cloneNode(true)
    angular.bootstrap document.body, ['expenses']

  restartAngular()

@runApp()
