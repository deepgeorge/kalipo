/**
 * Created by markus on 16.12.14.
 */
angular.module('kalipoApp')
    .directive('commentMenu', function ($compile, $templateCache, $http) {
    return {
        restrict: 'E', //E = element, A = attribute, C = class, M = comment
//        scope: {},
//        template: '<div ng-click="replaceMe()">I am the menu</div>',
        templateUrl: 'scripts/comment/partial_menu.html',
        replace: true,
//        transclude: true,
        controller: function ($scope, $element) {

            $scope.toggleReplyForm = function (comment) {
                $scope.$replyTo = comment.id;
                comment.report = false;
            };

            $scope.toggleReportForm = function (comment) {
                $scope.$replyTo = null;
                comment.report = !comment.report;
            };

            //$scope.replaceMe = function() {
            //    var html = $templateCache.get('scripts/thread/partial_menu.html');
            //    console.log(html);
            //    html ='<div>I should not be red</div>';
            //    var e = $compile(html)($scope);
            //    $element.replaceWith(e);
            //}
        }
    }
});