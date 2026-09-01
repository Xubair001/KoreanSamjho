# -*- coding: utf-8 -*-
"""Builds the course/lesson curriculum tree and the practice question bank
from the authored content. Lessons reference content by filter, never by copy,
so adding vocabulary automatically enriches the matching lesson."""
import json, os, random, glob
random.seed(20260901)
SRC = os.path.join(os.path.dirname(__file__), "..", "src")

def load(name, key):
    return json.load(open(os.path.join(SRC, name), encoding="utf-8"))[key]

vocab = []
for f in sorted(glob.glob(os.path.join(SRC, "vocabulary_*.json"))):
    vocab += json.load(open(f, encoding="utf-8"))["vocabulary"]
letters  = load("hangul_letters.json", "letters")
grammar  = load("grammar.json", "grammar")
sentences= load("sentences.json", "sentences")

# ---------------- Curriculum ----------------
def t(en, ur, hi): return {"en": en, "ur": ur, "hi": hi}

courses = [
 {"id":"course.hangul","track":"ALL","level":0,"order":1,
  "title":t("Level 0 — Getting Started","سطح ۰ — آغاز","स्तर 0 — शुरुआत"),
  "subtitle":t("Read Korean from zero","صفر سے کوریائی پڑھنا سیکھیں","शून्य से कोरियाई पढ़ना सीखें")},
 {"id":"course.beginner","track":"ALL","level":1,"order":2,
  "title":t("Beginner","ابتدائی","प्रारंभिक"),
  "subtitle":t("Greetings, numbers, family, everyday words","سلام، گنتی، خاندان، روزمرہ الفاظ","अभिवादन, गिनती, परिवार, रोज़मर्रा शब्द")},
 {"id":"course.elementary","track":"ALL","level":2,"order":3,
  "title":t("Elementary","بنیادی","बुनियादी"),
  "subtitle":t("Sentences you can use today","ایسے جملے جو آج ہی کام آئیں","ऐसे वाक्य जो आज ही काम आएँ")},
 {"id":"course.work","track":"EPS_EMPLOYMENT","level":3,"order":4,
  "title":t("Korean for Work","کام کے لیے کوریائی","काम के लिए कोरियाई"),
  "subtitle":t("Factory, site, safety and your manager","فیکٹری، سائٹ، حفاظت اور نگران","फ़ैक्ट्री, साइट, सुरक्षा और पर्यवेक्षक")},
 {"id":"course.academic","track":"TOPIK_ACADEMIC","level":3,"order":4,
  "title":t("Korean for TOPIK","ٹوپک کے لیے کوریائی","टॉपिक के लिए कोरियाई"),
  "subtitle":t("Grammar and reading for the exam","امتحان کے لیے گرامر اور مطالعہ","परीक्षा के लिए व्याकरण और पठन")},
 {"id":"course.life","track":"ALL","level":3,"order":5,
  "title":t("Everyday Life in Korea","کوریا میں روزمرہ زندگی","कोरिया में रोज़मर्रा जीवन"),
  "subtitle":t("Hospital, bank, shops, offices","ہسپتال، بینک، دکانیں، دفاتر","अस्पताल, बैंक, दुकानें, कार्यालय")},
]

lessons = []
def lesson(cid, order, title, kind, sel, level):
    lessons.append({"id": f"lesson.{cid.split('.')[1]}.{order:02d}", "course_id": cid,
                    "order": order, "title": title, "kind": kind,
                    "selector": sel, "level": level})

# Level 0 — Hangul, taught in the order Korean is actually taught
lesson("course.hangul",1,t("Basic vowels","بنیادی حروفِ علت","बुनियादी स्वर"),"hangul",{"kind":"vowel"},0)
lesson("course.hangul",2,t("Basic consonants","بنیادی حروفِ صحیح","बुनियादी व्यंजन"),"hangul",{"kind":"consonant"},0)
lesson("course.hangul",3,t("Building syllables","حرف جوڑ کر لفظ بنانا","अक्षर जोड़कर शब्द बनाना"),"syllable",{},0)
lesson("course.hangul",4,t("Double consonants","دوہرے حروفِ صحیح","द्विगुण व्यंजन"),"hangul",{"kind":"double_consonant"},0)
lesson("course.hangul",5,t("Compound vowels","مرکب حروفِ علت","संयुक्त स्वर"),"hangul",{"kind":"compound_vowel"},0)
lesson("course.hangul",6,t("Batchim — final consonants","بتچھم — آخری حروف","बतचिम — अंतिम व्यंजन"),"batchim",{},0)
lesson("course.hangul",7,t("Reading your first words","اپنے پہلے الفاظ پڑھنا","अपने पहले शब्द पढ़ना"),"vocab",{"level":1,"limit":12},0)

beg = [("greetings","Greetings","سلام دعا","अभिवादन"),("family","Family","خاندان","परिवार"),
       ("numbers","Numbers","گنتی","गिनती"),("time","Time and days","وقت اور دن","समय और दिन"),
       ("daily","Everyday verbs","روزمرہ افعال","रोज़मर्रा क्रियाएँ"),("food","Food and drink","کھانا پینا","खान-पान")]
for i,(cat,en,ur,hi) in enumerate(beg):
    lesson("course.beginner", i+1, t(en,ur,hi), "vocab", {"category":cat}, 1)
lesson("course.beginner", len(beg)+1, t("Saying who you are","اپنا تعارف","अपना परिचय"),"sentence",{"scenario":"self_intro"},1)

ele = [("be_formal","be_polite","Saying what something is","کچھ بتانا","कुछ बताना"),
       ("topic","subject","Topic and subject particles","موضوع اور فاعل کے نشان","विषय और कर्ता चिह्न"),
       ("object","loc_e","Objects and places","مفعول اور مقام","कर्म और स्थान"),
       ("loc_eseo","formal_present","Where you work","آپ کہاں کام کرتے ہیں","आप कहाँ काम करते हैं"),
       ("polite_present","past","Present and past","حال اور ماضی","वर्तमान और भूत"),
       ("neg_an","neg_ji_anta","Saying no","نفی کرنا","निषेध करना")]
for i,(g1,g2,en,ur,hi) in enumerate(ele):
    lesson("course.elementary", i+1, t(en,ur,hi), "grammar", {"ids":[f"grammar.{g1}",f"grammar.{g2}"]}, 2)
lesson("course.elementary", len(ele)+1, t("Shopping and eating","خریداری اور کھانا","खरीदारी और भोजन"),"sentence",{"scenario":"shopping"},2)
lesson("course.elementary", len(ele)+2, t("Getting around","آنا جانا","आना-जाना"),"vocab",{"category":"transport"},2)

work = [("vocab",{"category":"work"},"At the company","کمپنی میں","कंपनी में"),
        ("vocab",{"category":"factory"},"In the factory","فیکٹری میں","फ़ैक्ट्री में"),
        ("vocab",{"category":"construction"},"On the construction site","تعمیراتی سائٹ پر","निर्माण साइट पर"),
        ("vocab",{"category":"safety"},"Safety words that keep you safe","حفاظتی الفاظ","सुरक्षा शब्द"),
        ("sentence",{"scenario":"workplace_safety"},"Safety instructions","حفاظتی ہدایات","सुरक्षा निर्देश"),
        ("sentence",{"scenario":"workplace_manager"},"Talking to your manager","نگران سے بات","पर्यवेक्षक से बात"),
        ("sentence",{"scenario":"coworkers"},"Talking with coworkers","ساتھیوں سے بات","सहकर्मियों से बात"),
        ("grammar",{"ids":["grammar.must","grammar.dont"]},"Must and must not","کرنا ہوگا اور نہ کریں","करना होगा और न करें"),
        ("interview",{},"Interview practice","انٹرویو کی مشق","साक्षात्कार अभ्यास")]
for i,(k,sel,en,ur,hi) in enumerate(work):
    lesson("course.work", i+1, t(en,ur,hi), k, sel, 3)

acad = [("grammar",{"ids":["grammar.because_aseo","grammar.because_nikka"]},"Giving reasons","سبب بتانا","कारण बताना"),
        ("grammar",{"ids":["grammar.if","grammar.but"]},"Conditions and contrast","شرط اور تقابل","शर्त और तुलना"),
        ("grammar",{"ids":["grammar.can","grammar.want"]},"Ability and desire","صلاحیت اور خواہش","क्षमता और इच्छा"),
        ("grammar",{"ids":["grammar.honorific_si","grammar.request"]},"Respect and requests","احترام اور درخواست","आदर और अनुरोध"),
        ("grammar",{"ids":["grammar.and_go","grammar.also_do"]},"Joining ideas","خیالات جوڑنا","विचार जोड़ना"),
        ("vocab",{"level":3},"Intermediate vocabulary","درمیانی ذخیرۂ الفاظ","मध्यम शब्दावली")]
for i,(k,sel,en,ur,hi) in enumerate(acad):
    lesson("course.academic", i+1, t(en,ur,hi), k, sel, 3)

life = [("sentence",{"scenario":"hospital"},"At the hospital","ہسپتال میں","अस्पताल में"),
        ("sentence",{"scenario":"bank"},"At the bank","بینک میں","बैंक में"),
        ("sentence",{"scenario":"directions"},"Asking for directions","راستہ پوچھنا","रास्ता पूछना"),
        ("sentence",{"scenario":"restaurant"},"Ordering food","کھانے کا آرڈر","भोजन का ऑर्डर"),
        ("sentence",{"scenario":"immigration"},"At an official counter","سرکاری کاؤنٹر پر","सरकारी काउंटर पर"),
        ("vocab",{"category":"health"},"Health and the body","صحت اور جسم","स्वास्थ्य और शरीर"),
        ("vocab",{"category":"emergency"},"Emergencies","ہنگامی حالات","आपात स्थिति")]
for i,(k,sel,en,ur,hi) in enumerate(life):
    lesson("course.life", i+1, t(en,ur,hi), k, sel, 3)

json.dump({"content_version":1,"courses":courses,"lessons":lessons},
          open(os.path.join(SRC,"curriculum.json"),"w",encoding="utf-8"), ensure_ascii=False, indent=1)
print("courses:", len(courses), "lessons:", len(lessons))
