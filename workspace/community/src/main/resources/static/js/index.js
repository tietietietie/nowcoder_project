$(function(){
	$("#publishBtn").click(publish);
});


function publish() {
	$("#publishModal").modal("hide");
	//发送AJAX请求前，需要带上CSRF令牌
//    var token = $("mata[name='_csrf']").attr("content");
//    var header = $("mata[name='_csrf_header']").attr("content");
//    $(document).ajaxSend(function(e, xhr, options){
//        xhr.setRequestHeader(header, token);
//    });
	//先返回结果再显示
    //获取标题/内容
    var title =  $("#recipient-name").val();
    var content = $("#message-text").val();
    //发送异步请求
    $.post(
        "/community/discuss/add",
        {"title":title,"content":content},
        function(data){
            data = $.parseJSON(data);
            //提示框显示返回消息
            $("#hintBody").text(data.msg);
            //显示提示框/两秒后自动隐藏
            $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
                if(data.code == 0) {
                    window.location.reload();
                }
            }, 2000);
        }
    );
}
