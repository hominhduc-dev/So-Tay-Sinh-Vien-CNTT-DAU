"""Generate editable draw.io flowcharts for every active SRS use case."""
from pathlib import Path
from xml.etree.ElementTree import Element, SubElement, ElementTree, register_namespace
from xml.dom import minidom

OUT = Path(__file__).with_name("flowchart-use-cases.drawio")
register_namespace("", "")

PAGES = [
    ("UC01", "Đăng nhập", [
        ("start", "Bắt đầu", "start"), ("input", "Nhập mã sinh viên/cán bộ hoặc email và mật khẩu", "task"),
        ("check", "Tài khoản tồn tại, đang hoạt động, thuộc CNTT và mật khẩu đúng?", "decision"),
        ("bad", "Báo ‘Thông tin đăng nhập không đúng’\n(không tiết lộ lý do)", "error"),
        ("token", "Cấp JWT hiệu lực 8 giờ; trả thông tin và vai", "task"),
        ("route", "Điều hướng theo vai: chat / nội dung / quản trị", "task"), ("end", "Kết thúc", "end"),
    ], [("start","input",""),("input","check",""),("check","bad","Không"),("check","token","Có"),("token","route",""),("route","end",""),("bad","input","Thử lại")]),
    ("UC02", "Đăng xuất", [("start","Bắt đầu","start"),("click","Người dùng bấm Đăng xuất","task"),("clear","Client xóa token cục bộ\n(không gọi server)","task"),("login","Chuyển về trang đăng nhập","end")], [("start","click",""),("click","clear",""),("clear","login","")]),
    ("UC03", "Xem thông tin cá nhân", [("start","Mở trang cá nhân","start"),("auth","Gửi GET /api/auth/me; xác thực JWT và đọc tài khoản hiện hành","task"),("valid","Token hợp lệ, tài khoản hoạt động và thuộc CNTT?","decision"),("show","Trả và hiển thị họ tên, mã, email, vai, đơn vị","task"),("fail","401 · chuyển về trang đăng nhập","error")], [("start","auth",""),("auth","valid",""),("valid","show","Có"),("valid","fail","Không")]),
    ("UC04", "Đổi mật khẩu", [("start","Mở đổi mật khẩu","start"),("input","Nhập mật khẩu hiện tại và mật khẩu mới (8–72 ký tự)","task"),("valid","Token hợp lệ, mật khẩu mới hợp lệ?","decision"),("check","Kiểm tra mật khẩu hiện tại","task"),("match","Mật khẩu hiện tại đúng?","decision"),("save","Lưu mật khẩu mới dạng BCrypt; báo thành công","task"),("bad","Báo lỗi; không đổi mật khẩu","error")], [("start","input",""),("input","valid",""),("valid","check","Có"),("valid","bad","Không"),("check","match",""),("match","save","Đúng"),("match","bad","Sai")]),
    ("UC05", "Chọn câu hỏi, nhận trả lời", [("start","Mở chatbot","start"),("load","Tải chủ đề và hội thoại cá nhân","task"),("auth","Token còn hợp lệ?","decision"),("fail","401 · chuyển về đăng nhập","error"),("topic","Chọn chủ đề; tải các câu hỏi đang hiện","task"),("hasq","Chủ đề có câu hỏi?","decision"),("empty","Báo ‘Chủ đề chưa có nội dung’; quay lại chọn chủ đề","task"),("choose","Bấm nút câu hỏi có sẵn","task"),("active","Câu hỏi vẫn đang hiện?","decision"),("reload","404 · báo câu hỏi không còn; tải lại danh sách","error"),("save","Tạo hội thoại nếu cần; lưu lượt hỏi của người dùng","task"),("reply","Trả câu trả lời soạn sẵn, nguồn và trang; hiện nút chọn tiếp","end")], [("start","load",""),("load","auth",""),("auth","fail","Không"),("auth","topic","Có"),("topic","hasq",""),("hasq","empty","Không"),("hasq","choose","Có"),("empty","topic","Thử lại"),("choose","active",""),("active","reload","Không"),("active","save","Có"),("reload","topic","Tải lại"),("save","reply","")]),
    ("UC06", "Xem lại hội thoại", [("start","Mở danh sách hội thoại","start"),("list","Tải hội thoại của chính người dùng, mới nhất trước","task"),("select","Chọn một hội thoại","task"),("owner","Hội thoại thuộc người gọi?","decision"),("fail","401/404 · đăng nhập lại hoặc báo không tìm thấy","error"),("detail","Trả các lượt theo thời gian, kèm nguồn; câu hỏi đã ẩn vẫn hiện","task"),("show","Hiển thị lịch sử","end")], [("start","list",""),("list","select",""),("select","owner",""),("owner","fail","Không"),("owner","detail","Có"),("detail","show","")]),
    ("UC08", "Xem nguồn trích dẫn", [("start","Bấm nhãn nguồn trong câu trả lời","start"),("auth","Gửi yêu cầu tệp gốc kèm JWT","task"),("valid","Đăng nhập còn hợp lệ?","decision"),("fail","401 · yêu cầu đăng nhập lại","error"),("exists","Tài liệu và tệp còn tồn tại?","decision"),("missing","404 · báo không tìm thấy tệp","error"),("open","Trả tệp gốc: PDF mở tại trang N; DOCX mở/tải về","end")], [("start","auth",""),("auth","valid",""),("valid","fail","Không"),("valid","exists","Có"),("exists","missing","Không"),("exists","open","Có")]),
    ("UC07", "Tra cứu kho tài liệu", [("start","Mở kho tài liệu","start"),("auth","Xác thực request (mọi vai đều được đọc)","task"),("query","Nhập từ khóa tiêu đề tùy chọn (≤200 ký tự)","task"),("valid","Từ khóa hợp lệ?","decision"),("bad","400 · báo lỗi nhập liệu","error"),("search","Tìm theo tiêu đề; trả danh sách phân trang","task"),("show","Hiển thị metadata; chọn tệp để mở (UC08)","end")], [("start","auth",""),("auth","query",""),("query","valid",""),("valid","bad","Không"),("valid","search","Có"),("search","show","")]),
    ("UC10", "Tải lên tài liệu", [("start","Chọn PDF/DOCX và nhập tiêu đề","start"),("role","JWT hợp lệ và vai CONTENT_ADMIN trở lên?","decision"),("deny","401/403 · từ chối thao tác","error"),("valid","Tiêu đề hợp lệ; PDF/DOCX ≤20 MB?","decision"),("bad","400/413/415 · báo lỗi, không lưu","error"),("hash","Tính SHA-256 và kiểm tra trùng","task"),("dup","Đã tồn tại?","decision"),("duperr","409 · ‘Tài liệu đã tồn tại’","error"),("save","Lưu tệp tên ngẫu nhiên và metadata; đếm trang nếu PDF","task"),("end","Tài liệu dùng được làm nguồn UC23","end")], [("start","role",""),("role","deny","Không"),("role","valid","Có"),("valid","bad","Không"),("valid","hash","Có"),("hash","dup",""),("dup","duperr","Có"),("dup","save","Không"),("save","end","")]),
    ("UC11", "Sửa thông tin tài liệu", [("start","Chọn tài liệu và sửa tiêu đề","start"),("role","Token hợp lệ và có quyền CONTENT_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("valid","Tiêu đề 3–500 ký tự?","decision"),("bad","400 · báo lỗi nhập liệu","error"),("exists","Tài liệu tồn tại?","decision"),("missing","404 · không tìm thấy","error"),("save","Cập nhật tiêu đề; tệp không bị thay thế","end")], [("start","role",""),("role","deny","Không"),("role","valid","Có"),("valid","bad","Không"),("valid","exists","Có"),("exists","missing","Không"),("exists","save","Có")]),
    ("UC12", "Gỡ tài liệu", [("start","Chọn Gỡ tài liệu","start"),("role","Có quyền CONTENT_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("confirm","Người dùng xác nhận?","decision"),("cancel","Hủy · không thay đổi","task"),("used","Còn câu hỏi (kể cả ẩn) dẫn tới tài liệu?","decision"),("blocked","409 · chuyển nguồn câu hỏi trước","error"),("delete","Xóa bản ghi và tệp gốc","end")], [("start","role",""),("role","deny","Không"),("role","confirm","Có"),("confirm","cancel","Không"),("confirm","used","Có"),("used","blocked","Có"),("used","delete","Không")]),
    ("UC22", "Quản lý chủ đề", [("start","Mở quản lý chủ đề","start"),("role","Token và vai CONTENT_ADMIN trở lên?","decision"),("deny","401/403 · từ chối","error"),("action","Chọn thêm, sửa tên/thứ tự hoặc xóa","task"),("valid","Thêm/sửa: tên ≤100 ký tự, không trùng?","decision"),("bad","400/409 · sửa dữ liệu","error"),("used","Nếu xóa: chủ đề còn câu hỏi?","decision"),("blocked","409 · chuyển/xóa câu hỏi trước","error"),("save","Lưu thay đổi chủ đề","task"),("end","Có hiệu lực ở lần chatbot tải kế tiếp","end")], [("start","role",""),("role","deny","Không"),("role","action","Có"),("action","valid","Thêm/sửa"),("valid","bad","Không"),("valid","save","Có"),("action","used","Xóa"),("used","blocked","Có"),("used","save","Không"),("save","end","")]),
    ("UC23", "Quản lý câu hỏi – câu trả lời", [("start","Chọn chủ đề","start"),("role","Token và vai CONTENT_ADMIN trở lên?","decision"),("deny","401/403 · từ chối","error"),("list","Tải mọi câu hỏi, kể cả câu hỏi ẩn","task"),("action","Chọn thêm/sửa hoặc ẩn/hiện lại","task"),("fields","Nhập câu hỏi, câu trả lời, chủ đề, tài liệu nguồn, trang, thứ tự","task"),("valid","Chủ đề/tài liệu tồn tại, dữ liệu hợp lệ, trang trong phạm vi?","decision"),("bad","400 · báo lỗi từng trường; sửa rồi gửi lại","error"),("save","Lưu bản ghi; ẩn chỉ đổi trạng thái, không xóa","task"),("end","Câu hỏi active hiện thành nút UC05; lịch sử cũ vẫn đọc được","end")], [("start","role",""),("role","deny","Không"),("role","list","Có"),("list","action",""),("action","fields","Thêm/sửa"),("fields","valid",""),("valid","bad","Sai"),("valid","save","Đúng"),("action","save","Ẩn/hiện"),("bad","fields","Sửa"),("save","end","")]),
    ("UC14", "Tra cứu người dùng", [("start","Mở danh sách tài khoản","start"),("role","JWT hợp lệ và vai SYSTEM_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("query","Nhập từ khóa mã/email/họ tên (≤200 ký tự, tùy chọn)","task"),("valid","Từ khóa hợp lệ?","decision"),("bad","400 · lỗi dữ liệu","error"),("search","Tìm và phân trang tài khoản","task"),("end","Hiển thị vai, đơn vị, trạng thái","end")], [("start","role",""),("role","deny","Không"),("role","query","Có"),("query","valid",""),("valid","bad","Không"),("valid","search","Có"),("search","end","")]),
    ("UC15", "Khóa / mở tài khoản", [("start","Chọn tài khoản và trạng thái cần đặt","start"),("role","JWT hợp lệ và SYSTEM_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("self","Tài khoản mục tiêu là chính mình?","decision"),("blocked","400 SELF_MODIFICATION","error"),("exists","Tài khoản tồn tại?","decision"),("missing","404 · không tìm thấy","error"),("update","Cập nhật is_active; hiệu lực từ request kế tiếp (BR02)","end")], [("start","role",""),("role","deny","Không"),("role","self","Có"),("self","blocked","Có"),("self","exists","Không"),("exists","missing","Không"),("exists","update","Có")]),
    ("UC16", "Phân quyền", [("start","Chọn tài khoản và vai mới","start"),("role","JWT hợp lệ và SYSTEM_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("valid","Vai thuộc USER / CONTENT_ADMIN / SYSTEM_ADMIN?","decision"),("bad","400 · vai không hợp lệ","error"),("self","Mục tiêu là chính quản trị viên hiện tại?","decision"),("blocked","400 SELF_MODIFICATION","error"),("update","Cập nhật role; quyền mới hiệu lực từ request kế tiếp","end")], [("start","role",""),("role","deny","Không"),("role","valid","Có"),("valid","bad","Không"),("valid","self","Có"),("self","blocked","Có"),("self","update","Không")]),
    ("UC20", "Kiểm tra trạng thái hệ thống", [("start","Mở trang trạng thái","start"),("role","JWT hợp lệ và SYSTEM_ADMIN?","decision"),("deny","401/403 · từ chối","error"),("probe","Thử SELECT 1 trên PostgreSQL","task"),("db","CSDL phản hồi?","decision"),("up","database=UP; status=UP","end"),("down","database=DOWN; status=DEGRADED; endpoint vẫn trả 200","end")], [("start","role",""),("role","deny","Không"),("role","probe","Có"),("probe","db",""),("db","up","Có"),("db","down","Không")]),
]

STYLES = {
    "start": "ellipse;aspect=fixed;fillColor=#d5e8d4;strokeColor=#82b366;",
    "end": "ellipse;shape=doubleEllipse;fillColor=#d5e8d4;strokeColor=#82b366;",
    "task": "rounded=1;arcSize=12;fillColor=#dae8fc;strokeColor=#6c8ebf;",
    "decision": "rhombus;fillColor=#fff2cc;strokeColor=#d6b656;",
    "error": "rounded=1;arcSize=12;fillColor=#f8cecc;strokeColor=#b85450;",
}

def cell(container, tag, **attrs):
    return SubElement(container, tag, {k.replace("_", "-"): str(v) for k, v in attrs.items()})

def build_page(root, uc, name, nodes, edges):
    d = SubElement(root, "diagram", {"id": uc, "name": f"{uc} · {name}"})
    width = 1400
    height = max(760, 110 + len(nodes)*105)
    model = SubElement(d, "mxGraphModel", {"dx":str(width), "dy":str(height), "grid":"1", "gridSize":"10", "guides":"1", "tooltips":"1", "connect":"1", "arrows":"1", "fold":"1", "page":"1", "pageScale":"1", "pageWidth":str(width), "pageHeight":str(height), "math":"0", "shadow":"0"})
    r = SubElement(model, "root")
    cell(r, "mxCell", id="0"); cell(r, "mxCell", id="1", parent="0")
    cell(r, "mxCell", id="title", value=f"{uc} · {name}", style="text;html=1;align=center;verticalAlign=middle;fontStyle=1;fontSize=20;fontFamily=Arial;", vertex="1", parent="1")
    SubElement(r[-1], "mxGeometry", {"x":"250", "y":"20", "width":"700", "height":"45", "as":"geometry"})
    # Main spine follows the explicit first-successor chain; side outcomes sit beside
    # their decision, so connectors do not cross through later steps or nodes.
    node_map={nid:(label,kind) for nid,label,kind in nodes}
    outgoing={nid:[] for nid,_,_ in nodes}
    incoming={nid:[] for nid,_,_ in nodes}
    for src,dst,label in edges:
        outgoing[src].append((dst,label)); incoming[dst].append((src,label))
    main_next={}
    for nid,_,_ in nodes:
        opts=outgoing[nid]
        unlabeled=[dst for dst,label in opts if not label]
        if unlabeled: main_next[nid]=unlabeled[0]
        elif opts:
            # Prefer the successful/normal path over errors and alternate branches.
            preferred=next((dst for dst,label in opts if label in {"Có","Đúng","Hợp lệ","Thêm/sửa","Sửa"}),None)
            main_next[nid]=preferred or opts[-1][0]
    starts=[nid for nid,_,kind in nodes if kind=="start"]
    main=set(); cur=starts[0] if starts else nodes[0][0]
    while cur not in main:
        main.add(cur)
        if cur not in main_next: break
        cur=main_next[cur]
    main_order=[]; cur=starts[0] if starts else nodes[0][0]
    seen=set()
    while cur not in seen:
        seen.add(cur); main_order.append(cur)
        if cur not in main_next: break
        cur=main_next[cur]
    positions={}
    spine_x=500
    for ri,nid in enumerate(main_order):
        label,kind=node_map[nid]; y=100+ri*105
        w=290 if kind!="decision" else 330; h=66 if kind!="decision" else 100
        x=spine_x+(w-290)//2
        positions[nid]=(x,y,w,h)
        c=cell(r,"mxCell",id=nid,value=label,style=STYLES[kind]+"whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=14;",vertex="1",parent="1")
        SubElement(c,"mxGeometry",{"x":str(x),"y":str(y),"width":str(w),"height":str(h),"as":"geometry"})
    side_index=0
    for nid,label,kind in nodes:
        if nid in main: continue
        parents=[(src,edge_label) for src,edge_label in incoming[nid] if src in main]
        parent,label_edge=parents[0] if parents else (None,"")
        if parent and parent in positions:
            px,py,pw,ph=positions[parent]
            # Errors and negative outcomes go left; secondary successful branches go right.
            side_x=80 if kind=="error" or label_edge in {"Không","Sai"} else 1000
            siblings=[x for x,_,k in nodes if x not in main and any(s==parent for s,_ in incoming[x])]
            order=siblings.index(nid) if nid in siblings else 0
            y=py+order*95
            if abs(y-py)<40: y=py
        else:
            side_x=80 if side_index%2==0 else 1000; y=100+side_index*100; side_index+=1
        w=260 if kind!="decision" else 300; h=66 if kind!="decision" else 100
        positions[nid]=(side_x,y,w,h)
        c=cell(r,"mxCell",id=nid,value=label,style=STYLES[kind]+"whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=14;",vertex="1",parent="1")
        SubElement(c,"mxGeometry",{"x":str(side_x),"y":str(y),"width":str(w),"height":str(h),"as":"geometry"})
    for i,(src,dst,label) in enumerate(edges):
        # Draw from source bottom to target top for the forward spine. Side paths
        # use explicit exit/entry points and orthogonal routing around the nodes.
        style="edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;fontFamily=Arial;fontSize=13;strokeWidth=2;labelBackgroundColor=#ffffff;"
        if src in positions and dst in positions:
            sx,sy,sw,sh=positions[src]; tx,ty,tw,th=positions[dst]
            if tx > sx+sw: style += "exitX=1;exitY=0.5;entryX=0;entryY=0.5;exitDx=0;exitDy=0;entryDx=0;entryDy=0;"
            elif tx+tw < sx: style += "exitX=0;exitY=0.5;entryX=1;entryY=0.5;exitDx=0;exitDy=0;entryDx=0;entryDy=0;"
            else: style += "exitX=0.5;exitY=1;entryX=0.5;entryY=0;exitDx=0;exitDy=0;entryDx=0;entryDy=0;"
        c=cell(r,"mxCell",id=f"edge-{i+1}",value=label,style=style,edge="1",parent="1",source=src,target=dst)
        SubElement(c,"mxGeometry",{"relative":"1","as":"geometry"})

def main():
    root=Element("mxfile",{"host":"app.diagrams.net","modified":"2026-11-09T00:00:00.000Z","agent":"Codex","version":"24.7.17","type":"device","pages":str(len(PAGES))})
    for p in PAGES: build_page(root,*p)
    raw=ElementTree(root)
    xml=minidom.parseString(__import__('xml.etree.ElementTree',fromlist=['tostring']).tostring(root,encoding="utf-8")).toprettyxml(indent="  ",encoding="UTF-8")
    OUT.write_bytes(xml)
    # Reparse and assert each page has unique cell IDs and all edges resolve.
    from xml.etree.ElementTree import parse
    check=parse(OUT).getroot()
    assert len(check.findall("diagram")) == len(PAGES)
    for page in check.findall("diagram"):
        cells=page.findall(".//mxCell")
        ids=[c.get("id") for c in cells]
        assert len(ids)==len(set(ids)), page.get("name")
        idset=set(ids)
        for c in cells:
            if c.get("edge")=="1":
                assert c.get("source") in idset and c.get("target") in idset, page.get("name")
    print(f"Created {OUT} with {len(PAGES)} editable flowchart pages.")

if __name__=="__main__": main()
