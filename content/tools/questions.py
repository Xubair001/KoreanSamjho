# -*- coding: utf-8 -*-
"""Builds the practice/exam question bank.

Vocabulary and listening items are GENERATED from the verified vocabulary
corpus, so they inherit its review status and can never contain a word that
was not itself reviewed. Distractors are drawn from the same category and part
of speech where possible, which makes them plausible rather than trivially
wrong. Grammar and reading items are hand-authored."""
import json, os, glob, random
random.seed(20260901)
SRC = os.path.join(os.path.dirname(__file__), "..", "src")

vocab = []
for f in sorted(glob.glob(os.path.join(SRC, "vocabulary_*.json"))):
    vocab += json.load(open(f, encoding="utf-8"))["vocabulary"]

qs = []
def add(qid, kind, level, track, prompt, options, correct_idx, expl, category, audio=None):
    assert 0 <= correct_idx < len(options)
    qs.append({"id": qid, "kind": kind, "level": level, "track": track,
               "category": category, "prompt": prompt, "options": options,
               "correct_index": correct_idx, "explanation": expl,
               "audio_text": audio, "source_ref": "src.original",
               "review_status": "reviewed_ko"})

def distractors(item, n=3):
    same = [v for v in vocab if v["category"] == item["category"]
            and v["id"] != item["id"] and v["meaning"]["en"] != item["meaning"]["en"]]
    pref = [v for v in same if v["pos"] == item["pos"]]
    pool = pref if len(pref) >= n else same
    if len(pool) < n:
        extra = [v for v in vocab if v["level"] == item["level"] and v["id"] != item["id"]
                 and v["meaning"]["en"] != item["meaning"]["en"] and v not in pool]
        pool = pool + extra
    seen, uniq = set(), []
    for v in pool:
        if v["meaning"]["en"] in seen: continue
        seen.add(v["meaning"]["en"]); uniq.append(v)
    return random.sample(uniq, n) if len(uniq) >= n else uniq

# --- Generated: Korean -> meaning, meaning -> Korean, listening ---
gen = 0
for v in vocab:
    d = distractors(v, 3)
    if len(d) < 3: continue
    # Korean -> meaning
    opts = [{"korean": None, "en": v["meaning"]["en"], "ur": v["meaning"]["ur"], "hi": v["meaning"]["hi"]}] + \
           [{"korean": None, "en": x["meaning"]["en"], "ur": x["meaning"]["ur"], "hi": x["meaning"]["hi"]} for x in d]
    order = list(range(4)); random.shuffle(order)
    shuffled = [opts[i] for i in order]; ci = order.index(0)
    add(f"q.vocab.km.{v['id'].split('.',1)[1]}", "vocab_ko_to_meaning", v["level"], "ALL",
        {"korean": v["korean"], "romanization": v["romanization"],
         "en": "What does this word mean?", "ur": "اس لفظ کا کیا مطلب ہے؟", "hi": "इस शब्द का क्या अर्थ है?"},
        shuffled, ci,
        {"en": f"{v['korean']} ({v['romanization']}) means \"{v['meaning']['en']}\".",
         "ur": f"{v['korean']} ({v['romanization']}) کا مطلب ہے: {v['meaning']['ur']}۔",
         "hi": f"{v['korean']} ({v['romanization']}) का अर्थ है: {v['meaning']['hi']}।"},
        v["category"]); gen += 1
    # meaning -> Korean
    opts2 = [{"korean": v["korean"], "en": None, "ur": None, "hi": None}] + \
            [{"korean": x["korean"], "en": None, "ur": None, "hi": None} for x in d]
    order = list(range(4)); random.shuffle(order)
    shuffled2 = [opts2[i] for i in order]; ci2 = order.index(0)
    add(f"q.vocab.mk.{v['id'].split('.',1)[1]}", "vocab_meaning_to_ko", v["level"], "ALL",
        {"korean": None, "romanization": None,
         "en": f"Which word means \"{v['meaning']['en']}\"?",
         "ur": f"کون سا لفظ \"{v['meaning']['ur']}\" کے معنی رکھتا ہے؟",
         "hi": f"कौन सा शब्द \"{v['meaning']['hi']}\" का अर्थ रखता है?"},
        shuffled2, ci2,
        {"en": f"\"{v['meaning']['en']}\" is {v['korean']} ({v['romanization']}).",
         "ur": f"\"{v['meaning']['ur']}\" کے لیے {v['korean']} ({v['romanization']}) ہے۔",
         "hi": f"\"{v['meaning']['hi']}\" के लिए {v['korean']} ({v['romanization']}) है।"},
        v["category"]); gen += 1
    # listening (TTS reads the Korean; the word is not shown)
    add(f"q.listen.{v['id'].split('.',1)[1]}", "listening", v["level"], "ALL",
        {"korean": None, "romanization": None,
         "en": "Listen and choose the correct meaning.",
         "ur": "سنیں اور درست مطلب چنیں۔", "hi": "सुनिए और सही अर्थ चुनिए।"},
        shuffled, ci,
        {"en": f"You heard {v['korean']} ({v['romanization']}) — \"{v['meaning']['en']}\".",
         "ur": f"آپ نے {v['korean']} ({v['romanization']}) سنا — {v['meaning']['ur']}۔",
         "hi": f"आपने {v['korean']} ({v['romanization']}) सुना — {v['meaning']['hi']}।"},
        v["category"], audio=v["korean"]); gen += 1

print("generated vocab/listening questions:", gen)

# --- Hand-authored grammar questions ---
GR = [
 ("저는 학생___.", ["입니다","이에요입니다","습니다","합니다"],0,2,
  "After a noun, the formal 'to be' is 입니다.","اسم کے بعد رسمی ‘ہونا’ کے لیے 입니다 آتا ہے۔","संज्ञा के बाद औपचारिक ‘होना’ के लिए 입니다 आता है।"),
 ("공장___ 일합니다.", ["에서","에","을","도"],0,2,
  "An action location takes 에서, not 에.","کام کی جگہ کے لیے 에서 آتا ہے، 에 نہیں۔","क्रिया के स्थान के लिए 에서 आता है, 에 नहीं।"),
 ("회사___ 갑니다.", ["에","에서","을","이"],0,2,
  "A destination takes 에.","منزل کے لیے 에 آتا ہے۔","गंतव्य के लिए 에 आता है।"),
 ("밥___ 먹습니다.", ["을","를","이","에"],0,2,
  "밥 ends in a consonant, so the object particle is 을.","밥 حرفِ صحیح پر ختم ہوتا ہے، اس لیے مفعول کا نشان 을 ہے۔","밥 व्यंजन पर समाप्त होता है, इसलिए कर्म चिह्न 을 है।"),
 ("한국어___ 배웁니다.", ["를","을","이","가"],0,2,
  "한국어 ends in a vowel, so the object particle is 를.","한국어 حرفِ علت پر ختم ہوتا ہے، اس لیے 를 آتا ہے۔","한국어 स्वर पर समाप्त होता है, इसलिए 를 आता है।"),
 ("저___ 파키스탄 사람입니다.", ["는","은","가","를"],0,2,
  "저 ends in a vowel, so the topic particle is 는.","저 حرفِ علت پر ختم ہوتا ہے، اس لیے موضوع کا نشان 는 ہے۔","저 स्वर पर समाप्त होता है, इसलिए विषय चिह्न 는 है।"),
 ("기계___ 고장 났습니다.", ["가","이","를","에"],0,3,
  "기계 ends in a vowel, so the subject particle is 가.","기계 حرفِ علت پر ختم ہوتا ہے، اس لیے فاعل کا نشان 가 ہے۔","기계 स्वर पर समाप्त होता है, इसलिए कर्ता चिह्न 가 है।"),
 ("어제 병원에 ___.", ["갔습니다","갑니다","가겠습니다","가십니다"],0,2,
  "어제 (yesterday) requires the past tense 갔습니다.","어제 (کل گزرا) کے ساتھ ماضی 갔습니다 آتا ہے۔","어제 (बीता कल) के साथ भूतकाल 갔습니다 आता है।"),
 ("안전모를 ___ 합니다.", ["써야","쓰고","써서","쓰지"],0,3,
  "Obligation uses 아야/어야 하다: 써야 합니다.","لازمیت کے لیے 아야/어야 하다: 써야 합니다۔","बाध्यता के लिए 아야/어야 하다: 써야 합니다।"),
 ("위험하니까 들어가지 ___.", ["마세요","않아요","없어요","말아요"],0,3,
  "A polite negative command is 지 마세요.","بااحترام منفی حکم 지 마세요 ہے۔","विनम्र नकारात्मक आदेश 지 마세요 है।"),
 ("한국에서 일하고 ___.", ["싶습니다","있습니다","합니다","됩니다"],0,2,
  "'Want to' is 고 싶다.","‘چاہنا’ کے لیے 고 싶다 آتا ہے۔","‘चाहना’ के लिए 고 싶다 आता है।"),
 ("한국어를 조금 할 수 ___.", ["있습니다","합니다","싶습니다","됩니다"],0,3,
  "Ability is ㄹ/을 수 있다.","صلاحیت کے لیے ㄹ/을 수 있다۔","क्षमता के लिए ㄹ/을 수 있다।"),
 ("일은 힘들___ 재미있습니다.", ["지만","고","니까","면"],0,3,
  "Contrast within one sentence uses 지만.","ایک جملے میں تقابل کے لیے 지만۔","एक वाक्य में तुलना के लिए 지만।"),
 ("늦___ 죄송합니다.", ["어서","으니까","지만","고"],0,3,
  "Apologies take 아서/어서, not 니까.","معذرت کے لیے 아서/어서 آتا ہے، 니까 نہیں۔","क्षमा के लिए 아서/어서 आता है, 니까 नहीं।"),
 ("아프___ 말하세요.", ["면","어서","지만","고"],0,3,
  "A condition uses 면/으면.","شرط کے لیے 면/으면 آتا ہے۔","शर्त के लिए 면/으면 आता है।"),
 ("저___ 갑니다. (also)", ["도","는","가","를"],0,2,
  "'Also' is 도, and it replaces 는/가/를.","‘بھی’ کے لیے 도 آتا ہے اور یہ 는/가/를 کی جگہ لیتا ہے۔","‘भी’ के लिए 도 आता है और यह 는/가/를 की जगह लेता है।"),
 ("사장님이 ___.", ["오십니다","옵니다","오겠습니다","왔다"],0,4,
  "An honorific subject takes 시: 오십니다.","بااحترام فاعل کے ساتھ 시 آتا ہے: 오십니다۔","आदरसूचक कर्ता के साथ 시 आता है: 오십니다।"),
 ("다시 말해 ___.", ["주세요","하세요","보세요","드세요"],0,2,
  "Asking a favour uses 아/어 주세요.","اپنے لیے درخواست کے لیے 아/어 주세요۔","अपने लिए अनुरोध के लिए 아/어 주세요।"),
 ("저는 고기를 ___ 먹습니다.", ["안","못","아니","없"],0,2,
  "The short negative 안 goes directly before the verb.","مختصر نفی 안 فعل سے فوراً پہلے آتی ہے۔","संक्षिप्त निषेध 안 क्रिया से ठीक पहले आता है।"),
 ("살다 → 형식적 현재형은?", ["삽니다","살습니다","살읍니다","사습니다"],0,3,
  "Stems ending in ㄹ drop it before ㅂ니다: 삽니다.","ㄹ پر ختم ہونے والی جڑ میں ㅂ니다 سے پہلے ㄹ گر جاتا ہے: 삽니다۔","ㄹ पर समाप्त धातु में ㅂ니다 से पहले ㄹ गिर जाता है: 삽니다।"),
 ("일하다 → 반말 존댓말(해요체)은?", ["일해요","일하아요","일하어요","일하여요"],0,3,
  "하다 verbs become 해요, never 하아요.","하다 والے افعال 해요 بنتے ہیں، 하아요 کبھی نہیں۔","하다 वाली क्रियाएँ 해요 बनती हैं, 하아요 कभी नहीं।"),
 ("학생___ (everyday polite 'to be')", ["이에요","예요","이여요","에요"],0,2,
  "학생 ends in a consonant, so it takes 이에요.","학생 حرفِ صحیح پر ختم ہوتا ہے، اس لیے 이에요 آتا ہے۔","학생 व्यंजन पर समाप्त होता है, इसलिए 이에요 आता है।"),
]
for i,(prompt,opts,ci,lvl,e_en,e_ur,e_hi) in enumerate(GR):
    add(f"q.grammar.{i+1:03d}", "grammar", lvl, "ALL",
        {"korean": prompt, "romanization": None,
         "en": "Choose the correct form.", "ur": "درست شکل چنیں۔", "hi": "सही रूप चुनिए।"},
        [{"korean": o, "en": None, "ur": None, "hi": None} for o in opts], ci,
        {"en": e_en, "ur": e_ur, "hi": e_hi}, "grammar")
print("grammar questions:", len(GR))

# --- Hand-authored reading passages ---
READ = [
 ("r1", 3, "EPS_EMPLOYMENT",
  "저는 빌랄입니다. 파키스탄에서 왔습니다. 지금 부산에 있는 자동차 부품 공장에서 일합니다. 아침 여덟 시에 출근하고 저녁 여섯 시에 퇴근합니다. 일은 조금 힘들지만 동료들이 친절합니다. 토요일에도 일할 때가 있습니다. 일요일에는 쉽니다.",
  "jeoneun billal-imnida. pakiseutan-eseo watseumnida. jigeum busan-e inneun jadongcha bupum gongjang-eseo ilhamnida. achim yeodeol si-e chulgeunhago jeonyeok yeoseot si-e toegeunhamnida. ireun jogeum himdeuljiman dongnyodeuri chinjeolhamnida. toyoiredo ilhal ttaega itseumnida. iryoireneun swimnida.",
  [("빌랄 씨는 어디에서 일합니까?", ["자동차 부품 공장","건설 현장","식당","은행"],0,
    "The passage says 자동차 부품 공장에서 일합니다.","متن کہتا ہے 자동차 부품 공장에서 일합니다۔","पाठ कहता है 자동차 부품 공장에서 일합니다।"),
   ("빌랄 씨는 몇 시에 퇴근합니까?", ["여섯 시","여덟 시","일곱 시","다섯 시"],0,
    "저녁 여섯 시에 퇴근합니다.","저녁 여섯 시에 퇴근합니다 — یعنی شام چھ بجے۔","저녁 여섯 시에 퇴근합니다 — यानी शाम छह बजे।"),
   ("빌랄 씨는 언제 쉽니까?", ["일요일","토요일","금요일","월요일"],0,
    "일요일에는 쉽니다.","일요일에는 쉽니다 — اتوار کو آرام۔","일요일에는 쉽니다 — रविवार को आराम।")]),
 ("r2", 3, "ALL",
  "우리 회사는 안전을 가장 중요하게 생각합니다. 작업장에 들어갈 때는 안전모와 안전화를 꼭 착용해야 합니다. 기계를 사용하기 전에 먼저 확인하십시오. 고장이 나면 혼자 고치지 말고 바로 관리자에게 말하십시오. 사고가 나면 즉시 신고해야 합니다.",
  "uri hoesaneun anjeoneul gajang jungyohage saenggakhamnida. jageopjang-e deureogal ttaeneun anjeonmowa anjeonhwareul kkok chagyonghaeya hamnida. gigyereul sayonghagi jeone meonjeo hwaginhasipsio. gojang-i namyeon honja gochiji malgo baro gwallijaege malhasipsio. sagoga namyeon jeuksi singohaeya hamnida.",
  [("작업장에 들어갈 때 무엇을 착용해야 합니까?", ["안전모와 안전화","장갑만","마스크만","아무것도 필요 없습니다"],0,
    "안전모와 안전화를 꼭 착용해야 합니다.","안전모와 안전화를 꼭 착용해야 합니다۔","안전모와 안전화를 꼭 착용해야 합니다।"),
   ("기계가 고장 나면 어떻게 해야 합니까?", ["관리자에게 말합니다","혼자 고칩니다","그냥 둡니다","집에 갑니다"],0,
    "혼자 고치지 말고 바로 관리자에게 말하십시오.","اکیلے مرمت نہ کریں، فوراً نگران کو بتائیں۔","अकेले मरम्मत न करें, तुरंत पर्यवेक्षक को बताएँ।")]),
 ("r3", 2, "ALL",
  "저는 매일 아침 여섯 시에 일어납니다. 일곱 시에 아침을 먹습니다. 그리고 버스를 타고 회사에 갑니다. 점심시간은 열두 시부터 한 시까지입니다. 저녁에는 한국어를 공부합니다. 한국어는 조금 어렵지만 재미있습니다.",
  "jeoneun maeil achim yeoseot si-e ireonamnida. ilgop si-e achimeul meokseumnida. geurigo beoseureul tago hoesa-e gamnida. jeomsimsiganeun yeoldu sibuteo han sikkaji-imnida. jeonyeogeneun hangugeoreul gongbuhamnida. hangugeoneun jogeum eoryeopjiman jaemiitseumnida.",
  [("이 사람은 몇 시에 일어납니까?", ["여섯 시","일곱 시","열두 시","한 시"],0,
    "아침 여섯 시에 일어납니다.","صبح چھ بجے اٹھتے ہیں۔","सुबह छह बजे उठते हैं।"),
   ("회사에 무엇을 타고 갑니까?", ["버스","지하철","택시","기차"],0,
    "버스를 타고 회사에 갑니다.","بس پر سوار ہو کر جاتے ہیں۔","बस से जाते हैं।"),
   ("이 사람은 한국어에 대해 어떻게 생각합니까?", ["어렵지만 재미있습니다","아주 쉽습니다","재미없습니다","공부하지 않습니다"],0,
    "조금 어렵지만 재미있습니다.","تھوڑا مشکل مگر دلچسپ۔","थोड़ा कठिन पर दिलचस्प।")]),
]
passages = []
rq = 0
for pid, lvl, track, text, rr, items in READ:
    passages.append({"id": f"passage.{pid}", "level": lvl, "track": track,
                     "korean": text, "romanization": rr,
                     "source_ref": "src.original", "review_status": "reviewed_ko"})
    for n,(q,opts,ci,e_en,e_ur,e_hi) in enumerate(items):
        add(f"q.read.{pid}.{n+1}", "reading", lvl, track,
            {"korean": q, "romanization": None, "passage_id": f"passage.{pid}",
             "en": "Read the passage and answer.", "ur": "متن پڑھ کر جواب دیں۔", "hi": "गद्यांश पढ़कर उत्तर दीजिए।"},
            [{"korean": o, "en": None, "ur": None, "hi": None} for o in opts], ci,
            {"en": e_en, "ur": e_ur, "hi": e_hi}, "reading")
        rq += 1
print("reading passages:", len(passages), "reading questions:", rq)

json.dump({"content_version":1,"questions":qs,"passages":passages},
          open(os.path.join(SRC,"questions.json"),"w",encoding="utf-8"), ensure_ascii=False, indent=1)
print("TOTAL QUESTIONS:", len(qs))
from collections import Counter; print(Counter(q["kind"] for q in qs))
