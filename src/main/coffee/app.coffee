body = document.body.cloneNode(true)

@runApp = ->
  log = (args...) -> console?.log?(args...)

  app = angular.module 'expenses', ['ngResource']

  capitalize = (s) ->
    s[0].toUpperCase() + s.split('').slice(1).join('')

  app.factory "Expense", ['$resource', (resource) ->
    resource "/api/users/:userId/expenses/:id",
      userId: '@ownerId'
      id: '@id'
  ]

  clearForm = (scope) ->
    scope.form.$setPristine()
    delete scope.errorMessage
    scope.login = {
      email: ''
      password: ''
    }

  app.directive 'loginForm', ['$http', (http) ->
    scope: true
    link:
      (scope, element, attrs) ->
        scope.$watch 'loginType', (v) ->
          scope.loginButton = capitalize(v) if v?

        # autofill workaround
        setInterval ->
          elems = element.find('input');
          if elems.length > 0
            elems.triggerHandler('input')
              .triggerHandler('change')
              .triggerHandler('keydown')
        , 1000

        element.on 'submit', ->
          scope.$apply ->
            scope.dataFlow = true
          http.post("/api/users/#{scope.login.email}/#{scope.loginType}", scope.login)
            .success (data, status, headers, config) ->
              scope.$parent.user = data
              scope.dataFlow = false
              clearForm(scope)
              scope.$root.view = 'welcome-screen'
            .error (data, status, headers, config) ->
              scope.dataFlow = false
              scope.errorMessage = data.code
  ]

  app.directive 'dateControl', [() ->
    link: (scope, element, attrs) ->
      setTimeout ->
        $(element[0]).datetimepicker()
      10 # give a chance to get right value in the input
      element[0].datafilter = (v) -> moment(v).valueOf()
  ]

  app.directive 'floatValue', [() ->
    link: (scope, element, attrs) ->
      element[0].datafilter = (v) ->
        res = parseFloat(v)
        res = 0 if isNaN(res)
        res
  ]

  app.directive 'edits', [() ->
    link: (scope, element, attrs) ->
      element.on 'blur', ->
        filter = element[0].datafilter || (v) -> v
        scope.$apply ->
          scope.e[attrs['edits']] = filter(element.val())
  ]

  app.directive 'expenseView', [() ->
    link: (scope, element, attrs) ->
      scope.$watchCollection 'e', (v, old) ->
        editing = v.editing
        if editing
          element.attr 'title', ''
        else
          element.attr 'title', 'Click to edit'

        if !angular.equals(v, old)
          scope.updating = true
          v.$save ->
            scope.updating = false
            v.editing = editing
  ]

  app.controller 'expensesController',
    ['$scope', '$http', 'Expense', (scope, http, Expense) ->

      scope.finishEdit = () ->
        angular.forEach scope.expenses, (e) ->
          delete e.editing
        scope.expenses.sort (a, b) ->
          b.timestamp - a.timestamp

      scope.$watch "user", (user) ->
        if user?
          Expense.query
            userId: user.id
            , (data) ->
              scope.expenses = data

      scope.logout = ->
        scope.dataFlow = true
        http.post("/api/users/#{scope.user.id}/logout", "")
          .success (data, status, headers, config) ->
            scope.dataFlow = false
            scope.$root.user = null
            scope.expenses = null

      scope.addExpense = ->
        scope.expenses ||= []
        scope.addingExpense = true
        e =
          timestamp: new Date().getTime()
          comment: ''
          description: ''
          amount: 0
          ownerId: scope.user.id
        Expense.save(e)
        .$promise.then (e) ->
          scope.addingExpense = false
          e.editing = true
          scope.expenses.unshift e
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
