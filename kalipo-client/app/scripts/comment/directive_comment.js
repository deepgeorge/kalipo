/**
 * Created by markus on 16.12.14.
 */
angular.module('kalipoApp')
    .directive('comment', function () {
    return {
        restrict: 'E',
        replace: false,
        //transclude: true,
        //scope: {},
        templateUrl: 'scripts/comment/partial_comment.html'
    }
});
