/**
 * Created by markus on 16.12.14.
 */
angular.module('kalipoApp')
    .directive('comment', function () {
    return {
        restrict: 'E',
        replace: false,
        templateUrl: 'scripts/comment/partial_comment.html'
    }
});
