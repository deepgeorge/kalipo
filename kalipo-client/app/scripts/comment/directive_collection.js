/**
 * Created by markus on 16.12.14.
 */
angular.module('kalipoApp')
    .directive('collection', function ($compile, $templateCache, $http, $rootScope, Vote, Comment) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                collection: '=',
                page: '='
            },
            template: '',
            link: function ($scope, $element, $attrs) {

                $scope.draft = {};

                // modal to create comments
                $http.get('views/partial_reply.html', {cache: true}).success(function (tmpl_reply) {
                    $element.append($compile(tmpl_reply)($scope));
                });

                $http.get('views/partial_comment.html', {cache: true}).success(function (tmpl_comment) {
                    $http.get('views/partial_menu.html', {cache: true}).success(function (tmpl_menu) {

                        var compiled_comment = _.template(tmpl_comment);
                        var compiled_menu = _.template(tmpl_menu);
                        var compiled_dropped = _.template('<div class="dropped" style="border-left: <%= level * 10 %>px solid #ececec;"><a href="javascript:void(0)" ng-click="getDroppedComments(\'<%= ids %>\')">View <strong><%= count %></strong> <% if(count==1) { %>reply<% } else { %>replies<% } %></a> <span class="glyphicon glyphicon-chevron-down"></span></div>');

                        var $thread = $('<div></div>');

                        var __render = function (comment, $sink) {

                            var $comment = $(compiled_comment({
                                comment: comment,
                                page: $scope.page,
                                fnRenderMenu: compiled_menu
                            })).appendTo($sink);

                            if (_.isArray(comment.replies.verbose)) {
                                var $replies = $('<div></div>', {class: 'replies'}).appendTo($comment);

                                _.forEach(comment.replies.verbose, function (reply) {
                                    __render(reply, $replies);
                                });
                            }

                            if (comment.replies.dropped.length > 0) {
                                // todo fix ids
                                $comment.append(compiled_dropped({
                                    count: comment.$repliesCount,
                                    level: comment.level,
                                    ids: comment.replies.dropped.join(',')
                                }));
                            }

                        };

                        _.forEach($scope.collection, function (comment) {
                            __render(comment, $thread);
                        });

                        $element.append($compile($thread.contents())($scope));

                    });
                });

                $scope.getDroppedComments = function (commentIds) {
                    console.log('fetch dropped comments of', commentIds)
                };

                $scope.showReplyModal = function (commentId, displayName, threadId, quote) {
                    $('#createCommentModal').modal();
                    $scope.displayName = displayName;
                    $scope.draft.threadId = threadId;
                    $scope.draft.body = '>' + quote.replace(/\n/g, '>\n');
                    $scope.draft.parentId = commentId;
                };

                $scope.toggleReportForm = function (commentId) {
                    console.log('report', commentId);
                    // todo show popover
                };

                $scope.submitComment = function () {

                    console.log('submit comment', $scope.draft);
                    // todo support anon flag in view
                    $scope.draft.anonymous = false;

                    Comment.save($scope.draft,
                        function () {
                            // todo notification
                        });
                };

                $scope.verbose = function (commentId) {
                    $('#comment-' + commentId).removeClass('oneline');
                };

                $scope.toggleReplies = function (commentId) {
                    console.log('event:discussion-changed -> ...');
                    $('#comment-' + commentId).toggleClass('hiddenreplies');
                    $rootScope.$broadcast('event:discussion-changed');
                };

                $scope.like = function (commentId) {
                    console.log('like', commentId);
                    commentId.likes++;

                    var vote = {like: true, commentId: commentId};

                    Vote.save(vote, function (updated) {
//                noty({text: 'Liked', type: 'success'});
                    });
                };

                $scope.dislike = function (commentId) {
                    console.log('dislike', commentId);
                    commentId.dislikes++;

                    var vote = {like: false, commentId: commentId};

                    Vote.save(vote, function (updated) {
//                noty({text: 'Disliked', type: 'success'});
                    });
                };

            }
        }
    });
