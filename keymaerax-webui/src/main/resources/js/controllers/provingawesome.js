////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Proving
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

angular.module('keymaerax.controllers').controller('ProofCtrl',
    function($scope, $rootScope, $http, $route, $routeParams, $q, $uibModal, $timeout,
             sequentProofData, spinnerService, sessionService) {

  $scope.userId = sessionService.getUser();
  $scope.proofId = $routeParams.proofId;
  sequentProofData.clear(); // @note we load a new proof, so clear agenda and proof tree

  $scope.taskExplanation = {
    selection: "Rule"
  };
  $scope.stepAxiom = function() {
        var selectedItem = sequentProofData.agenda.selectedItem()
        if (selectedItem) {
          var explanationNodeId = selectedItem.deduction.sections[0].path[0];
          var node = sequentProofData.proofTree.node(explanationNodeId);
          return [node.rule];
        } else return [];
      }

  $scope.intro.introOptions = {
    steps: [
    {
        element: '#provingautomation',
        intro: "Automatic proof search. Unfold all operators automatically. Undo proof steps.",
        position: 'bottom'
    },
    {
        element: '#provingbasictactics',
        intro: "Basic tactics for propositional reasoning, hybrid programs, differential equations, and arithmetic are applied somewhere in the goal.",
        position: 'bottom'
    },
    {
        element: '#provingadditionaltools',
        intro: "Advanced proof tools (inspection, finding counterexamples, synthesizing assumptions).",
        position: 'bottom'
    },
    {
        element: '#provingtab',
        intro: "Each unfinished branch of a proof is displayed on its own tab",
        position: 'bottom'
    },
    {
        element: '#provingsequentview',
        intro: "The sequent view shows the current open proof goal at the top. Hover over formulas to find out where tactics can be applied. Left-click to apply default proof tactic. Right-click for a list of tactics to choose from. Hover over <code>&#8866;</code> for tactics that work on the entire sequent.",
        position: 'bottom'
    },
    {
        element: '#provingtactics',
        intro: "Proofs can be programmed in addition to clicking. Learn tactic programming by observing how the tactic is built while you click in the sequent above. Augment by typing into the text box. Get auto-completion by typing a formula number <code>1.</code> followed by a dot. Either re-run the entire tactic from scratch, or execute the modifications only.",
        position: 'bottom'
    }
    ],
    showStepNumbers: false,
    exitOnOverlayClick: true,
    exitOnEsc: true,
    nextLabel: 'Next', // could use HTML in labels
    prevLabel: 'Previous',
    skipLabel: 'Exit',
    doneLabel: 'Done'
  }

  $http.get("/config/tool").success(function(data) {
    $scope.tool = data.tool;
  });

  $http.get('proofs/user/' + $scope.userId + "/" + $scope.proofId).success(function(data) {
      $scope.proofId = data.id;
      $scope.proofName = data.name;
      $scope.modelId = data.model;
      $scope.closed = data.closed;
      $scope.stepCount= data.stepCount;
      $scope.date = data.date;
      if (data.stepCount == 0 && data.tactic !== undefined && data.tactic !== null) {
        // imported but not yet executed proof
        spinnerService.show('tacticExecutionSpinner')
        $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/initfromtactic')
          .then(function(response) { $scope.runningTask.start($scope.proofId, '()', response.data.taskId, response.data.info,
                                     $scope.updateFreshProof, $scope.broadcastProofError, undefined); })
          .catch(function(err) {
            spinnerService.hide('tacticExecutionSpinner');
            $rootScope.$broadcast("proof.message", err.data);
          });
      } else {
        spinnerService.show('proofLoadingSpinner');
        sequentProofData.fetchAgenda($scope, $scope.userId, $scope.proofId);
      }
  });
  $scope.$emit('routeLoaded', {theview: 'proofs/:proofId'});

  $scope.$on('agenda.isEmpty', function(event, data) {
    if (data.proofId == $scope.proofId) {
      // the current controller is responsible
      $http.get('proofs/user/' + $scope.userId + "/" + $scope.proofId + '/progress').success(function(data) {
        if (data.status == 'closed') {
          var modalInstance = $uibModal.open({
            templateUrl: 'partials/prooffinisheddialog.html',
            controller: 'ProofFinishedDialogCtrl',
            size: 'lg',
            resolve: {
                userId: function() { return $scope.userId; },
                proofId: function() { return $scope.proofId; },
                proofName: function() { return $scope.proofName; }
            }
          });
        } else {
          // should never happen
          showMessage($uibModal, 'Empty agenda even though proof ' + $scope.proofId + ' is not closed (' + data.status + ')')
        }
      });
    }
  });

  $scope.updateFreshProof = function(taskResult) {
    if (taskResult.type === 'taskresult') {
      if ($scope.proofId === taskResult.proofId) {
        if ($scope.runningTask.nodeId === taskResult.parent.id) {
          $route.reload();
        } else {
          showMessage($uibModal, "Unexpected tactic result, parent mismatch: expected " +
            $scope.runningTask.nodeId + " but got " + taskResult.parent.id);
        }
      } else {
        showMessage($uibModal, "Unexpected tactic result, proof mismatch: expected " +
          $scope.proofId + " but got " + taskResult.proofId);
      }
    } else {
      showCaughtErrorMessage($uibModal, taskResult, "Unable to fetch tactic result")
    }
  }

  $scope.updateMainProof = function(taskResult) {
    if (taskResult.type === 'taskresult') {
      if ($scope.proofId === taskResult.proofId) {
        if ($scope.runningTask.nodeId === taskResult.parent.id) {
          $rootScope.$broadcast('proof.message', { textStatus: "", errorThrown: "" });
          sequentProofData.updateAgendaAndTree($scope.userId, taskResult.proofId, taskResult);
          sequentProofData.tactic.fetch($scope.userId, taskResult.proofId);
        } else {
          showMessage($uibModal, "Unexpected tactic result, parent mismatch: expected " +
            $scope.runningTask.nodeId + " but got " + taskResult.parent.id);
        }
      } else {
        showMessage($uibModal, "Unexpected tactic result, proof mismatch: expected " +
          $scope.proofId + " but got " + taskResult.proofId);
      }
    } else {
      showCaughtErrorMessage($uibModal, taskResult, "Unable to fetch tactic result")
    }
  }

  $scope.broadcastProofError = function(err) {
    $rootScope.$broadcast('proof.message', {
      errorThrown: err.data.errorThrown,
      textStatus: err.data.textStatus,
      causeMsg: err.data.causeMsg,
      tacticMsg: err.data.tacticMsg,
      taskStepwiseRequest: $scope.runningTask.taskStepwiseRequest
    })
  }

  $scope.runningTask = {
    proofId: undefined,
    nodeId: undefined,
    taskId: undefined,
    taskStepwiseRequest: undefined,
    future: undefined,
    lastStep: undefined,
    info: undefined,
    start: function(proofId, nodeId, taskId, info, onTaskComplete, onTaskError, taskStepwiseRequest) {
      $scope.runningTask.proofId = proofId;
      $scope.runningTask.nodeId = nodeId;
      $scope.runningTask.taskId = taskId;
      $scope.runningTask.info = info;
      $scope.runningTask.taskStepwiseRequest = taskStepwiseRequest;
      $scope.runningTask.future = $q.defer();
      $scope.runningTask.future.promise.then(
        /* future resolved */ function(taskId) {
          $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + taskId + '/result')
            .then(function(response) { onTaskComplete(response.data); })
            .catch(function(err) { onTaskError(err); })
            .finally(function() { spinnerService.hide('tacticExecutionSpinner'); });
        },
        /* future rejected */ function(reason) {
          $rootScope.$broadcast('proof.message', { textStatus: "", errorThrown: "" });
          if (reason !== 'stopped') showMessage($uibModal, reason);
          spinnerService.hide('tacticExecutionSpinner');
        }
      );
      $scope.runningTask.poll(taskId, 0);
    },
    poll: function(taskId, elapsed) {
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + taskId + '/status')
        .then(function(response) {
          if (response.data.lastStep !== undefined) $scope.runningTask.lastStep = response.data.lastStep.ruleName;
          if (response.data.status === 'done') $scope.runningTask.future.resolve(taskId);
          else if (elapsed <= 20) $timeout(function() { $scope.runningTask.poll(taskId, elapsed+1); }, 50);
          else $timeout(function() { $scope.runningTask.poll(taskId, elapsed); }, 1000);
        })
        .catch(function(error) { $scope.runningTask.future.reject(error); });
    },
    stop: function() {
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + $scope.runningTask.taskId + '/stop')
        .then(function(response) {
          if ($scope.runningTask.future) $scope.runningTask.future.reject('stopped');
        })
        .catch(function(err) {
          if ($scope.runningTask.future) $scope.runningTask.future.reject(err);
        });
    }
  }
});

angular.module('keymaerax.controllers').controller('InitBrowseProofCtrl',
    function($scope, $rootScope, $http, $routeParams, $q, $uibModal, $timeout, sequentProofData, spinnerService, sessionService) {

  $scope.proofId = $routeParams.proofId;
  $scope.userId = sessionService.getUser();
  sequentProofData.clear(); // @note we load a new proof, so clear agenda and proof tree

  $http.get('proofs/user/' + $scope.userId + "/" + $scope.proofId).success(function(data) {
      $scope.proofId = data.id;
      $scope.proofName = data.name;
      $scope.modelId = data.model;
      $scope.closed = data.closed;
      $scope.stepCount= data.stepCount;
      $scope.date = data.date;
      if (data.stepCount == 0 && data.tactic !== undefined && data.tactic !== null) {
        // imported but not yet executed proof
        spinnerService.show('tacticExecutionSpinner')
        $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/initfromtactic')
          .then(function(response) { $scope.runningTask.start($scope.proofId, '()', response.data.taskId,
                                     $scope.updateFreshProof, $scope.broadcastProofError, undefined); })
          .catch(function(err) {
            spinnerService.hide('tacticExecutionSpinner');
            $rootScope.$broadcast("proof.message", err.data);
          });
      } else {
        sequentProofData.fetchBrowseAgenda($scope, $scope.userId, $scope.proofId);
      }
  });
  $scope.$emit('routeLoaded', {theview: 'proofs/:proofId/browse'});

  $scope.updateFreshProof = function(taskResult) {
    if (taskResult.type === 'taskresult') {
      if ($scope.proofId === taskResult.proofId) {
        if ($scope.runningTask.nodeId === taskResult.parent.id) {
          sequentProofData.fetchBrowseAgenda($scope, $scope.userId, $scope.proofId);
        } else {
          showMessage($uibModal, "Unexpected tactic result, parent mismatch: expected " +
            $scope.runningTask.nodeId + " but got " + taskResult.parent.id);
        }
      } else {
        showMessage($uibModal, "Unexpected tactic result, proof mismatch: expected " +
          $scope.proofId + " but got " + taskResult.proofId);
      }
    } else {
      showCaughtErrorMessage($uibModal, taskResult, "Unable to fetch tactic result")
    }
  }

  $scope.broadcastProofError = function(err) {
    $rootScope.$broadcast('proof.message', {
      errorThrown: err.data.errorThrown,
      textStatus: err.data.textStatus,
      causeMsg: err.data.causeMsg,
      tacticMsg: err.data.tacticMsg,
      taskStepwiseRequest: $scope.runningTask.taskStepwiseRequest
    })
  }

  //@todo task service (see also ProofCtrl)
  $scope.runningTask = {
    proofId: undefined,
    nodeId: undefined,
    taskId: undefined,
    taskStepwiseRequest: undefined,
    future: undefined,
    lastStep: undefined,
    info: undefined,
    start: function(proofId, nodeId, taskId, info, onTaskComplete, onTaskError, taskStepwiseRequest) {
      $scope.runningTask.proofId = proofId;
      $scope.runningTask.nodeId = nodeId;
      $scope.runningTask.taskId = taskId;
      $scope.runningTask.info = info;
      $scope.runningTask.taskStepwiseRequest = taskStepwiseRequest;
      $scope.runningTask.future = $q.defer();
      $scope.runningTask.future.promise.then(
        /* future resolved */ function(taskId) {
          $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + taskId + '/result')
            .then(function(response) { onTaskComplete(response.data); })
            .catch(function(err) { onTaskError(err); })
            .finally(function() { spinnerService.hide('tacticExecutionSpinner'); });
        },
        /* future rejected */ function(reason) {
          $rootScope.$broadcast('proof.message', { textStatus: "", errorThrown: "" });
          if (reason !== 'stopped') showMessage($uibModal, reason);
          spinnerService.hide('tacticExecutionSpinner');
        }
      );
      $scope.runningTask.poll(taskId, 0);
    },
    poll: function(taskId, elapsed) {
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + taskId + '/status')
        .then(function(response) {
          if (response.data.lastStep !== undefined) $scope.runningTask.lastStep = response.data.lastStep.ruleName;
          if (response.data.status === 'done') $scope.runningTask.future.resolve(taskId);
          else if (elapsed <= 20) $timeout(function() { $scope.runningTask.poll(taskId, elapsed+1); }, 50);
          else $timeout(function() { $scope.runningTask.poll(taskId, elapsed); }, 1000);
        })
        .catch(function(error) { $scope.runningTask.future.reject(error); });
    },
    stop: function() {
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.runningTask.proofId + '/' + $scope.runningTask.nodeId + '/' + $scope.runningTask.taskId + '/stop')
        .then(function(response) { $scope.runningTask.future.reject('stopped'); })
        .catch(function(err) { $scope.runningTask.future.reject(err); });
    }
  }
});

angular.module('keymaerax.controllers').controller('BrowseProofCtrl',
    function($scope, $rootScope, $http, $routeParams, $q, $uibModal, $timeout, sequentProofData, spinnerService, sessionService) {

  $scope.proofId = $routeParams.proofId;
  $scope.userId = sessionService.getUser();
  $scope.agenda = sequentProofData.agenda;
  $scope.prooftree = sequentProofData.proofTree;
});

angular.module('keymaerax.controllers').controller('TaskCtrl',
  function($rootScope, $scope, $http, $route, $routeParams, $q, $uibModal, Tactics, sequentProofData, spinnerService,
           derivationInfos, sessionService, Poller) {
    $scope.proofId = $routeParams.proofId;
    $scope.userId = sessionService.getUser();
    $scope.agenda = sequentProofData.agenda;
    $scope.prooftree = sequentProofData.proofTree;
    $scope.tactic = sequentProofData.tactic;
    $scope.backend = {
      busypoller: Poller.poll("tools/vitalSigns", 2000 /*2s*/),
      connectionTestResult: undefined
    };
    sequentProofData.tactic.reset();

    $scope.$on('$destroy', function() {
        $scope.backend.busypoller.data.cancel = true;
    });

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Subsection on tree operations.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    $scope.editLabel = function(node) {
        node.editing = true
    }

    $scope.saveLabel = function(node) {
        //TODO save the label.... http.put....
        node.editing = false
    }

    $scope.cancelEditing = function(node) {
        node.editing = false
    }

    $scope.toggle = function(scope) { scope.toggle() } // do need this.

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Subsection on executing tasks
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    $scope.fetchNodeInfo = function(dispatched) {
      var uri = "/proofs/user/" + $scope.userId + "/" + dispatched.proofId + "/agendaDetails/" + dispatched.nodeId;
      $http.get(uri)
        .success(function(data) {
        data.readOnly = true;
        $scope.selectedTask = data;
      });
    }

    $scope.undoLastStep = function() {
      var nodeId = sequentProofData.agenda.selectedId();
      var node = sequentProofData.agenda.itemsMap[nodeId];
      var top = node.deduction.sections[0].path[0];
      var topParent = sequentProofData.proofTree.nodesMap[top].parent;
      sequentProofData.prune($scope.userId, $scope.proofId, topParent);
    };

    $scope.setFormulaMode = function(mode) {
      sequentProofData.formulas.mode = mode;
    }

    $scope.getFormulaMode = function() {
      return sequentProofData.formulas.mode;
    }

    $scope.stickyEdit = function() {
      return sequentProofData.formulas.mode == 'edit' && sequentProofData.formulas.stickyEdit;
    }

    $scope.setStickyEdit = function(stickyEdit) {
      sequentProofData.formulas.stickyEdit = stickyEdit;
    }

    $scope.exportSubgoal = function() {
        var nodeId = sequentProofData.agenda.selectedId();

        var uri = 'proofs/user/export/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId

        $http.get(uri)
            .then(function(response) {
                if(response.data.errorThrown) {
                    showCaughtErrorMessage($uibModal, response.data.message, response.data)
                }
                else {
                    showVerbatimMessage($uibModal, "Exported Subgoal", response.data.sequent);
                }
            })
    }

    $scope.stepwiseTactic = function(stepwiseRequest) {
      spinnerService.show('magnifyingglassSpinner')
      $http(stepwiseRequest).then(function(response) {
        var onStepwiseTaskComplete = function(taskResult) {
          $http.get('proofs/user/' + $scope.userId + '/' + taskResult.proofId + '/trace').then(function(response) {
            var modalInstance = $uibModal.open({
              templateUrl: 'templates/magnifyingglass.html',
              controller: 'MagnifyingGlassDialogCtrl',
              scope: $scope,
              size: 'magnifyingglass',
              resolve: {
                proofInfo: function() {
                  return {
                    userId: $scope.userId,
                    proofId: "", //@note irrelevant for dialog
                    nodeId: "",  //@note irrelevant for dialog
                    detailsProofId: response.data.detailsProofId
                  }
                },
                tactic: function() { return response.data.tactic; },
                proofTree: function() { return response.data.proofTree; },
                openGoals: function() { return response.data.openGoals; }
              }
            });
          })
          .finally(function() {
            spinnerService.hide('magnifyingglassSpinner');
          });
        }

        var onStepwiseTaskError = function(err) {
          spinnerService.hide('magnifyingglassSpinner');
          $uibModal.open({
            templateUrl: 'templates/modalMessageTemplate.html',
            controller: 'ModalMessageCtrl',
            size: 'sm',
            resolve: {
              title: function() { return "Immediate error"; },
              message: function() { return "Tactic did not make progress at all"; }
            }
          });
        }

        $scope.runningTask.start(response.data.proofId, response.data.nodeId, response.data.taskId, response.data.info,
          onStepwiseTaskComplete, onStepwiseTaskError);
      })
      .catch(function(err) {
        spinnerService.hide('magnifyingglassSpinner');
        showCaughtTacticErrorMessage($uibModal, err.data.errorThrown, err.data.textStatus, err.data.tacticMsg);
      });
    }

    $scope.openProofstepBrowser = function() {
      var prevMode = sequentProofData.formulas.mode;
      sequentProofData.formulas.mode = 'select';
      var modalInstance = $uibModal.open({
        templateUrl: 'partials/lemmabrowserdialog.html',
        controller: 'LemmaBrowserCtrl',
        size: 'lg',
        resolve: {
          userId: function() { return $scope.userId; },
          proofId: function() { return $scope.proofId; },
          nodeId: function() { return sequentProofData.agenda.selectedId(); },
          formulaId: function() { return undefined; },
          formula: function() { return undefined; }
        }
      });
      modalInstance.result.then(
        function (tactic) {
          sequentProofData.formulas.mode = prevMode;
          if (tactic.input) $scope.doInputTactic(tactic.formulaId, tactic.tacticId, tactic.input);
          else $scope.doTactic(tactic.formulaId, tactic.tacticId);
        },
        function () { sequentProofData.formulas.mode = prevMode; }
      );
    }

    $scope.doTactic = function(formulaId, tacticId) {
      var nodeId = sequentProofData.agenda.selectedId();
      var base = 'proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId;
      var uri = formulaId !== undefined ?  base + '/' + formulaId + '/doAt/' + tacticId : base + '/do/' + tacticId;
      var stepwise = { method: 'GET', url: uri + '?stepwise=true' };
      spinnerService.show('tacticExecutionSpinner')
      $http.get(uri + '?stepwise=false')
        .then(function(response) { $scope.runningTask.start($scope.proofId, nodeId, response.data.taskId, response.data.info, $scope.updateMainProof, $scope.broadcastProofError, stepwise); })
        .catch(function(err) {
          spinnerService.hide('tacticExecutionSpinner');
          $rootScope.$broadcast("proof.message", err.data);
        });
    }

    $scope.doInputTactic = function(formulaId, tacticId, input) {
      var nodeId = sequentProofData.agenda.selectedId();
      spinnerService.show('tacticExecutionSpinner');
      var base = 'proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId;
      var uri = formulaId !== undefined ? base + '/' + formulaId + '/doInputAt/' + tacticId : base + '/doInput/' + tacticId
      var stepwise = { method: 'POST', url: uri + '?stepwise=true', data: input};
      $http.post(uri + '?stepwise=false', input)
        .then(function(response) { $scope.runningTask.start($scope.proofId, nodeId, response.data.taskId, response.data.info, $scope.updateMainProof, $scope.broadcastProofError, stepwise); })
        .catch(function(err) {
          spinnerService.hide('tacticExecutionSpinner');
          $rootScope.$broadcast("proof.message", err.data);
        });
    }

    $scope.doTwoPositionTactic = function(fml1Id, fml2Id, tacticId) {
      var nodeId = sequentProofData.agenda.selectedId();
      spinnerService.show('tacticExecutionSpinner');
      var uri = 'proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId + '/' + fml1Id + '/' + fml2Id + '/doAt/' + tacticId;
      var stepwise = { method: 'GET', url: uri + '?stepwise=true' };
      $http.get(uri + '?stepwise=false')
        .then(function(response) { $scope.runningTask.start($scope.proofId, nodeId, response.data.taskId, response.data.info, $scope.updateMainProof, $scope.broadcastProofError, stepwise); })
        .catch(function(err) {
          spinnerService.hide('tacticExecutionSpinner');
          $rootScope.$broadcast("proof.message", err.data);
        });
    }

    $scope.doSearch = function(tacticId, where) { doSearchImpl(tacticId, where, undefined); }
    $scope.doSearchInput = function(tacticId, where, input) { doSearchImpl(tacticId, where, input); }
    doSearchImpl = function(tacticId, where, input) {
      var nodeId = sequentProofData.agenda.selectedId();
      spinnerService.show('tacticExecutionSpinner');
      var uri = 'proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId + '/doSearch/' + where + '/' + tacticId;
      var stepwise = input !== undefined ? { method: 'POST', url: uri + '?stepwise=true', data: input } : { method: 'GET', url: uri + '?stepwise=true' };
      var request = input !== undefined ? $http.post(uri + '?stepwise=false', input) : $http.get(uri + '?stepwise=false')
      request.then(function(response) { $scope.runningTask.start($scope.proofId, nodeId, response.data.taskId, response.data.info, $scope.updateMainProof, $scope.broadcastProofError, stepwise); })
        .catch(function(err) {
          spinnerService.hide('tacticExecutionSpinner');
        });
    }

    $scope.onTacticScript = function(tacticText, stepwise) {
      var nodeId = sequentProofData.agenda.selectedId();
      if (nodeId != undefined) {
        if (tacticText != "nil") {
          spinnerService.show('tacticExecutionSpinner');
          var uri = 'proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + nodeId + '/doCustomTactic';
          var updateProof = stepwise ? $scope.updateFreshProof : $scope.updateMainProof
          $http.post(uri + '?stepwise='+stepwise, tacticText)
            .then(function(response) { $scope.runningTask.start($scope.proofId, nodeId, response.data.taskId, response.data.info,
                                       updateProof, $scope.broadcastProofError, undefined); })
            .catch(function(err) {
              spinnerService.hideAll();
              if (err.data.errorThrown != undefined) {
                //@note errors that occur before scheduling (parsing etc.), but not tactic execution errors -> cannot repeat from here
                $rootScope.$broadcast('proof.message', err.data);
              } else {
                console.error("Expected errorThrown field on error object but found something else: " + JSON.stringify(err));
              }
            });
        } // else nothing to do
      } else {
        console.error("Undefined selected node in agenda when trying to run the tactic script '" + tacticText + "'");
      }
    }

    $scope.openInputTacticDialog = function(tacticName, positionLocator) {
      var nodeId = sequentProofData.agenda.selectedId();
      var tactics = derivationInfos.byName($scope.userId, $scope.proofId, nodeId, tacticName)
        .then(function(response) {
          return response.data;
        });

      var modalInstance = $uibModal.open({
        templateUrl: 'templates/derivationInfoDialog.html',
        controller: 'DerivationInfoDialogCtrl',
        size: 'lg',
        resolve: {
          tactics: function() { return tactics; },
          readOnly: function() { return false; }
        }
      });

      modalInstance.result.then(function(derivation) {
        if (positionLocator === undefined) $scope.doInputTactic(undefined, tacticName, derivation);
        else $scope.doSearchInput(tacticName, positionLocator, derivation);
      })
    }

    $scope.openTacticEditor = function() {
      $uibModal.open({
        templateUrl: 'templates/tacticEditor.html',
        controller: 'TacticEditorCtrl',
        size: 'lg',
        resolve: {
          parentScope: function() { return $scope; }
        }
      })
    }

    $scope.rulehelp = {
      codeName: undefined
    };

    $scope.fetchRuleHelp = function(codeName) {
      $scope.rulehelp.codeName = codeName;
      // return name of the ng-template in proofawesesome.html
      return 'rulehelp.html';
    }

    $scope.executeTacticDiff = function(stepwise) {
      if ($scope.tactic.tacticDel === '' || $scope.tactic.tacticDel === 'nil') {
        $scope.onTacticScript($scope.tactic.tacticDiff, stepwise);
      } else {
        $scope.rerunTactic();
      }
    };

    $scope.rerunTactic = function() {
      var tactic = $scope.tactic.tacticText;
      sequentProofData.prune($scope.userId, $scope.proofId, $scope.prooftree.root, function() {
        $scope.onTacticScript(tactic, true);
      });
    }

    $scope.simulate = function() {
      $uibModal.open({
        templateUrl: 'templates/simulator.html',
        controller: 'SimulatorCtrl',
        size: 'lg',
        resolve: {
          proofId: function() { return $scope.proofId; },
          userId: function() { return $scope.userId; },
          nodeId: function() { return sequentProofData.agenda.selectedId(); }
        }
      })
    }

    //@todo duplicate with sequent.js#getCounterExample
    $scope.getCounterExample = function() {
      spinnerService.show('counterExampleSpinner');
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + $scope.agenda.selectedId() + '/counterExample')
        .then(function(response) {
          var dialogSize = (response.data.result === 'cex.found') ? 'lg' : 'md';
          $uibModal.open({
            templateUrl: 'templates/counterExample.html',
            controller: 'CounterExampleCtrl',
            size: dialogSize,
            resolve: {
              result: function() { return response.data.result; },
              origFormula: function() { return response.data.origFormula; },
              cexFormula: function() { return response.data.cexFormula; },
              cexValues: function() { return response.data.cexValues; },
              speculatedValues: function() { return response.data.speculatedValues; }
            }
          });
        })
        .finally(function() { spinnerService.hide('counterExampleSpinner'); });
    }

    $scope.getODEConditions = function() {
      spinnerService.show('odeConditionsSpinner');
      $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + $scope.agenda.selectedId() + '/odeConditions')
        .then(function(response) {
          $uibModal.open({
            templateUrl: 'templates/odeConditions.html',
            controller: 'ODEConditionsCtrl',
            size: 'lg',
            resolve: {
              sufficient: function() { return response.data.sufficient; },
              necessary: function() { return response.data.necessary; }
            }
          });
        })
        .catch(function(err) {
          $uibModal.open({
            templateUrl: 'templates/modalMessageTemplate.html',
            controller: 'ModalMessageCtrl',
            size: 'md',
            resolve: {
              title: function() { return "Unable to find ODE conditions"; },
              message: function() { return err.data.textStatus; }
            }
          })
        })
        .finally(function() { spinnerService.hide('odeConditionsSpinner'); });
    }

    $scope.getPegasusODECandidates = function() {
          spinnerService.show('odeConditionsSpinner');
          $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/' + $scope.agenda.selectedId() + '/pegasusCandidates')
            .then(function(response) {
              $uibModal.open({
                templateUrl: 'templates/pegasusCandidates.html',
                controller: 'PegasusCandidatesCtrl',
                size: 'lg',
                resolve: {
                  candidates: function() { return response.data.candidates; }
                }
              });
            })
            .catch(function(err) {
              $uibModal.open({
                templateUrl: 'templates/modalMessageTemplate.html',
                controller: 'ModalMessageCtrl',
                size: 'md',
                resolve: {
                  title: function() { return "Unable to find Pegasus invariant candidates"; },
                  message: function() { return err.data.textStatus; }
                }
              })
            })
            .finally(function() { spinnerService.hide('odeConditionsSpinner'); });
        }

    $scope.downloadProblemSolution = function() {
        $http.get('proofs/user/' + $scope.userId + '/' + $scope.proofId + '/download').success(function (data) {
            $uibModal.open({
                templateUrl: 'templates/tacticExtracted.html',
                controller: 'TacticExtractionCtrl',
                size: 'lg',
                resolve: {
                    tacticText: function () {
                        return data.fileContents;
                    }
                }
            })
        })
    }

    $scope.restartBackend = function() { $http.get("tools/restart"); }
    $scope.testBackendConnection = function() {
      $http.get("tools/testConnection").then(function(response) {
        $scope.backend.connectionTestResult = true;
        $uibModal.open({
          templateUrl: 'templates/modalMessageTemplate.html',
          controller: 'ModalMessageCtrl',
          size: 'md',
          resolve: {
            title: function() { return "Connection test successful"; },
            message: function() { return "The tool connection is operational."; }
          }
        })
      })
      .catch(function(err) {
        $scope.backend.connectionTestResult = false;
        $uibModal.open({
          templateUrl: 'templates/modalMessageTemplate.html',
          controller: 'ModalMessageCtrl',
          size: 'md',
          resolve: {
            title: function() { return "Error testing connection"; },
            message: function() { return err.data.textStatus; }
          }
        })
      })
    }
      
    //Save a name edited using the inline editor.
    $scope.saveProofName = function(newName) {
      $http.post("proofs/user/" + $scope.userId + "/" + $scope.proofId + "/name/" + newName, {})
    }

    $scope.saveTaskName = function(newName) {
      var nodeId = sequentProofData.agenda.selectedId();
      if (nodeId != undefined) $http.post("proofs/user/" + $scope.userId + "/" + $scope.proofId + "/" + nodeId + "/name/" + newName, {});
    }

    $scope.openModelEditor = function (modelId) {
        var modalInstance = $uibModal.open({
          templateUrl: 'partials/modeldialog.html',
          controller: 'ModelDialogCtrl',
          size: 'fullscreen',
          resolve: {
            userid: function() { return $scope.userId; },
            modelid: function() { return modelId; },
            mode: function() { return 'proofedit'; },
            proofid: function() { return $scope.proofId; }
          }
        });
      };
  });

angular.module('keymaerax.controllers').controller('ProofFinishedDialogCtrl',
        function($scope, $http, $uibModalInstance, $location, FileSaver, Blob, userId, proofId, proofName) {

    // empty open proof until fetched from server
    $scope.proof = {
      proofId: '',
      checking: true,
      //isProved: true/false is reported from server
      tactic: '',
      provable: ''
    }

    // fetch proof
    $http.get("/proofs/user/" + userId + "/" + proofId + "/validatedStatus").then(function(response) {
      $scope.proof = response.data; // no transformation, pass on to HTML as is
    });

    // just close the dialog
    $scope.cancel = function() { $uibModalInstance.dismiss('cancel'); };

    $scope.browseProof = function() {
      $uibModalInstance.dismiss('cancel');
      $location.path('/proofs/' + $scope.proof.proofId + '/browse');
    };

    // don't trust local cache, fetch new from server
    //@todo duplicate with proofs.js downloadTactic
    $scope.downloadTactic = function() {
      $http.get("/proofs/user/" + userId + "/" + proofId + "/extract").then(function(response) {
        var data = new Blob([response.data.tacticText], { type: 'text/plain;charset=utf-8' });
        FileSaver.saveAs(data, proofName + '.kyt');
      });
    }

    // don't trust local cache, fetch new from server
    //@todo duplicate with proofs.js downloadLemma
    $scope.downloadLemma = function() {
      $http.get("/proofs/user/" + userId + "/" + proofId + "/lemma").then(function(response) {
        var data = new Blob([response.data.fileContents], { type: 'text/plain;charset=utf-8' });
        FileSaver.saveAs(data, proofName + '.kyp');
      });
    }

    //@todo duplicate with proofs.js downloadPartialProof
    $scope.downloadProofArchive = function() {
      $http.get("/proofs/user/" + userId + "/" + proofId + "/download").then(function(response) {
        var data = new Blob([response.data.fileContents], { type: 'text/plain;charset=utf-8' });
        FileSaver.saveAs(data, proofName + '.kya');
      });
    }
});
