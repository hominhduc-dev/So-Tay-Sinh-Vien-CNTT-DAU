"""Rearrange only the ERD page in the consolidated draw.io file."""
from pathlib import Path
from xml.etree.ElementTree import parse, ElementTree, SubElement
from xml.dom import minidom

PATH = Path(__file__).with_name("so-do-he-thong.drawio")
tree = parse(PATH)
root = tree.getroot()
page = next(d for d in root.findall("diagram") if d.get("name") == "ERD hệ thống")
cells = {c.get("id"): c for c in page.findall(".//mxCell")}

# Logical arrangement: users feed conversations; conversation/messages/questions
# form the visual spine; topics and documents are reference branches into questions.
positions = {
    "users": (80, 150),
    "conversations": (570, 150),
    "messages": (570, 590),
    "questions": (1060, 590),
    "topics": (1060, 150),
    "documents": (80, 590),
}
for entity, (x, y) in positions.items():
    geom = cells[entity].find("mxGeometry")
    geom.set("x", str(x)); geom.set("y", str(y))

def route(edge_id, points):
    edge = cells[edge_id]
    geom = edge.find("mxGeometry")
    if geom is None:
        geom = SubElement(edge, "mxGeometry", {"relative":"1", "as":"geometry"})
    for child in list(geom):
        if child.tag == "Array": geom.remove(child)
    arr = SubElement(geom, "Array", {"as":"points"})
    for x, y in points:
        SubElement(arr, "mxPoint", {"x":str(x), "y":str(y)})

# Explicit orthogonal waypoints keep the six relationships out of entity boxes.
# Table width is 390; heights are retained from the existing ERD.
route("relation-0", [(470, 295), (520, 295), (520, 760), (80, 760)])
route("relation-1", [(470, 295), (570, 295)])
route("relation-2", [(1255, 300), (1255, 510), (1070, 510), (1070, 590)])
route("relation-3", [(275, 910), (1060, 910)])
route("relation-4", [(765, 334), (765, 590)])
route("relation-5", [(960, 682), (1060, 682)])

# Make the flow direction visually explicit while preserving endpoints and cardinality labels.
for edge_id in ("relation-1", "relation-2", "relation-3", "relation-4", "relation-5"):
    cells[edge_id].set("style", cells[edge_id].get("style", "").replace("endArrow=none", "endArrow=block").replace("startArrow=none", "startArrow=none"))

# Wider diagram frame and title align with the new left-to-right arrangement.
model = page.find("mxGraphModel")
model.set("dx", "1500"); model.set("dy", "1350")
model.set("pageWidth", "1500"); model.set("pageHeight", "1350")
title_geom = cells["title"].find("mxGeometry")
title_geom.set("x", "250"); title_geom.set("width", "1000")
for note_id in ("legend", "rules"):
    geom = cells[note_id].find("mxGeometry")
    geom.set("x", "80"); geom.set("width", "1370")

# Update page count from actual tabs and write atomically through a temp file.
root.set("pages", str(len(root.findall("diagram"))))
payload = minidom.parseString(__import__("xml.etree.ElementTree", fromlist=["tostring"]).tostring(root, encoding="utf-8")).toprettyxml(indent="  ", encoding="UTF-8")
tmp = PATH.with_suffix(".tmp")
tmp.write_bytes(payload); tmp.replace(PATH)

# Verify page names, foreign keys represented by edges, and parseability.
check = parse(PATH).getroot()
erd = next(d for d in check.findall("diagram") if d.get("name") == "ERD hệ thống")
assert len([c for c in erd.findall(".//mxCell") if c.get("edge") == "1" and c.get("id", "").startswith("relation-") and "cardinality" not in c.get("id", "")]) == 6
print("ERD page rearranged and validated.")
