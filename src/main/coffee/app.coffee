body = document.body.cloneNode(true)

#
# determinest start and end of the week given a year and week
#
week = (year, week, mondayStyle = false)->
  m = moment()

  m.startOf('year')
  m.startOf('week')

  if mondayStyle
    m.add(1, 'day')

  start = m.add(week - 1, 'week').subtract(m.zone(), 'minutes')
  end = moment(m).add(1, 'week').subtract(1, 'hour')

  [start.valueOf(), end.valueOf()]

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

  app.factory "WeekStat", ['$resource', (resource) ->
    resource "/api/users/:userId/expenses/stats"
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
      if scope.e.fresh
        delete scope.e.fresh
        scope.editing = true

      element.on 'click', (e) ->
        s = angular.element(e.target).scope()
        if (!s.editing)
          scope.finishEdit()
          s.$apply ->
            s.editing = true

      scope.$watchCollection 'e', (v, old) ->
        if !angular.equals(v, old)
          scope.$parent.updating = true
          v.$save ->
            scope.$parent.updating = false

      scope.deleteExpense = (event) ->
        event.stopPropagation()
        scope.e.$delete ->
          scope.refreshExpenses()
  ]

  app.controller 'expensesController',
    ['$scope', '$http', 'Expense', 'WeekStat', (scope, http, Expense, WeekStat) ->

      filterRefresher = -1
      scope.$watch 'filterStringInput', (v) ->
        clearTimeout filterRefresher
        filterRefresher = setTimeout ->
          scope.$apply ->
            scope.filterString = v
            scope.refreshExpenses(v)
        , 500

      scope.finishEdit = (e) ->
        e.stopPropagation() if (e)
        s = scope.$$childHead
        while s
          s.editing = false
          s = s.$$nextSibling
        scope.refreshExpenses() if (e)

      scope.refreshExpenses = ->
        return if !scope.user?
        scope.updating = true
        params = userId: scope.user.id
        params.filter = scope.filterString if scope.filterString?
        Expense.query params, (data) ->
          scope.updating = false
          scope.expenses = data

      scope.$watch "user", (user) ->
        scope.view = 'expenses'
        scope.refreshExpenses()

      scope.logout = ->
        scope.dataFlow = true
        http.post("/api/users/#{scope.user.id}/logout", "")
          .success (data, status, headers, config) ->
            scope.dataFlow = false
            scope.$root.user = null
            scope.expenses = null

      scope.addExpense = ->
        scope.finishEdit()
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
          e.fresh = true
          scope.expenses.unshift e

      scope.weekStats = ->
        scope.stats = null
        scope.view = 'weekStats'
        scope.updating = true
        params = userId: scope.user.id
        statWeek = (stat) -> week(stat.year, stat.week, false)
        WeekStat.query params, (data) ->
          scope.updating = false
          scope.stats = for d in data
            d.week = statWeek(d)
            d

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
