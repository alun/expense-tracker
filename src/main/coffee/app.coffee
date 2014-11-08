body = document.body.cloneNode(true)

@runApp = ->
  log = (args...) -> console?.log?(args...)

  app = angular.module 'expenses', ['ngResource']

  capitalize = (s) ->
    s[0].toUpperCase() + s.split('').slice(1).join('')

  ###
  app.factory "userApi", ['$resource', (resource) ->
    resource "/api/users/:id/:verb",
        id: '@username'
      ,
        login:
          method: 'POST'
          params: verb: 'login'
  ]
  ###

  clearForm = (scope) ->
    scope.form.$setPristine()
    delete scope.errorMessage
    scope.user = {
      email: ''
      password: ''
    }

  app.directive 'loginForm', ['$http', (http) ->
    scope: true
    link:
      (scope, element, attrs) ->
        scope.$watch 'loginType', (v) ->
          scope.loginButton = capitalize(v)

        element.on 'submit', ->
          scope.$apply ->
            scope.dataFlow = true
          http.post("/api/users/#{scope.user.email}/#{scope.loginType}", scope.user)
            .success (data, status, headers, config) ->
              scope.dataFlow = false
              clearForm(scope)
              scope.$root.view = 'welcome-screen'
            .error (data, status, headers, config) ->
              scope.dataFlow = false
              scope.errorMessage = data.code
  ]

  app.directive 'clearForm', ['$http', (http) ->
    (scope, element, attrs) ->
      element.on 'click', ->
        scope.$apply -> clearForm(scope)
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
