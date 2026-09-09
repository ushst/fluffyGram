#!/usr/bin/env python3
"""Rebuild fluffy smart_reply_db.json: intents + curated + cleaned legacy."""

from __future__ import annotations

import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "TMessagesProj/src/main/assets/fluffy/smart_reply_db.json"
OUT = SRC
LEARNING_INTENT = Path(
    __import__("os").environ.get(
        "FLUFFY_SMART_REPLY_INTENT_BANK",
        str(ROOT.parent / "learning/smart_reply_pack/intent_bank.json"),
    )
)

CATCH_ALL_REPLIES = {
    "мяу",
    "пу пу пу",
    "пупупу",
    "пока я сплю",
    "сплю",
    "пиздец",
    "блять",
    "бля",
    "фу",
    "эх",
    "окак",
    "умничка",
    "умничка моя",
    "сладенький",
    "моя ты умничка",
    "котёночек",
    "котеночек",
}

STOP = {
    "и", "в", "во", "на", "я", "ты", "он", "она", "мы", "вы",
    "то", "это", "а", "но", "да", "ну", "же", "ли", "бы", "к", "у",
    "с", "со", "о", "об", "от", "по", "из", "за", "для", "или",
    "если", "уже", "еще", "ещё", "мне", "тебе", "меня", "тебя",
    "мой", "моя", "просто", "очень", "про",
}

EXTRA_INTENTS = [
    {
        "id": "thanks",
        "patterns": ["спасибо", "благодар", "^спс$", "^пасиб$", "^сяб$"],
        "chips": ["Пожалуйста", "Да не за что", "Ок"],
    },
    {
        "id": "will_arrive",
        "patterns": [
            "буду через",
            "буду минут",
            "буду через минут",
            "приеду через",
            "буду через полчаса",
            "через \\d+ минут",
        ],
        "chips": ["Ок", "Жду", "Хорошо", "Ага"],
    },
    {
        "id": "laugh",
        "patterns": [
            "^а?ха+",
            "^хи+",
            "ахаха",
            "хахаха",
            "^лол$",
            "^кек$",
        ],
        "chips": ["Хаха", "😂", "Ага"],
    },
    {
        "id": "later",
        "patterns": [
            "давай позже",
            "потом напишу",
            "чуть позже",
            "не сейчас",
            "занят сейчас",
        ],
        "chips": ["Ок", "Хорошо", "Жду"],
    },
    {
        "id": "wait",
        "patterns": ["подожди", "погоди", "секунду", "минуту", "щас"],
        "chips": ["Ок", "Жду", "Ага"],
    },
    {
        "id": "question_generic",
        "patterns": ["^правда\\??$", "^серьёзно\\??$", "^серьезно\\??$", "^точно\\??$"],
        "chips": ["Да", "Ага", "Ну да"],
    },
    {
        "id": "miss_you_short",
        "patterns": ["^скучаю$", "я скучаю"],
        "chips": ["И я", "Я тоже", "Скоро увидимся"],
    },
    {
        "id": "good",
        "patterns": ["^отлично$", "^супер$", "^класс$", "^круто$", "^кайф$"],
        "chips": ["👍", "Ага", "Рад"],
    },
    {
        "id": "bye",
        "patterns": ["^пока$", "^до связи$", "^спокойной$", "увидимся"],
        "chips": ["Пока", "Давай", "До связи"],
    },
    {
        "id": "congrats",
        "patterns": ["поздравляю", "с днём", "с днем рождения"],
        "chips": ["Спасибо", "Благодарю", "🥰"],
    },
    {
        "id": "about_that",
        "patterns": [
            "хотела написать",
            "хотел написать",
            "как раз хотел",
            "как раз думал",
            "как раз про это",
        ],
        "chips": ["О чём?", "Расскажи", "Интересно"],
    },
]

CURATED_TEMPLATES = [
    # (contexts, replies)
    (["привет", "приветик", "хай", "ку", "здравствуй"], ["Привет", "Ку", "Хай"]),
    (["как дела", "как ты", "ты как", "как сам", "как ты там"], ["Нормально", "Все хорошо", "Пойдет", "Норм"]),
    (["спасибо", "благодарю", "спс", "пасиб"], ["Пожалуйста", "Да не за что", "Ок"]),
    (["ок", "окей", "хорошо", "договорились", "ладно"], ["Ок", "Ага", "👍"]),
    (["да", "ага", "угу", "точно"], ["Ок", "Ага", "Хорошо"]),
    (["нет", "неа", "не"], ["Ок", "Понял", "Ладно"]),
    (["ты где", "где ты", "где сейчас"], ["Еду", "Дома", "Скоро"]),
    (["когда будешь", "скоро будешь", "долго ещё", "долго еще"], ["Скоро", "Еду", "Хз"]),
    (["буду через 10 минут", "буду через 20 минут", "буду минут через 30", "приеду через час"], ["Ок", "Жду", "Хорошо"]),
    (["что делаешь", "чем занят", "чем занята"], ["Работаю", "Ничего", "Дома"]),
    (["спокойной ночи", "сладких снов", "ложись"], ["Спокойной", "И ты", "Сладких"]),
    (["доброе утро"], ["Доброе", "Доброе утро", "Привет"]),
    (["люблю тебя", "я тебя люблю", "скучаю"], ["И я тебя", "Я тоже", "🥰"]),
    (["извини", "прости", "сорри"], ["Ничего", "Бывает", "Ок"]),
    (["позвони", "набери", "перезвони"], ["Сейчас", "Ок", "Минуту"]),
    (["скинь фото", "пришли фото", "скинь фотку"], ["Сейчас", "Ок", "Позже"]),
    (["давай позже", "потом напишу", "чуть позже"], ["Ок", "Хорошо", "Жду"]),
    (["подожди", "погоди", "секунду", "минутку"], ["Ок", "Жду", "Ага"]),
    (["хахаха", "ахахаха", "лол"], ["Хаха", "😂", "Ага"]),
    (["ты занят", "занят?", "есть минута"], ["Ага", "Работаю", "Давай"]),
    (["увидимся", "давай встретимся", "встретимся"], ["Давай", "Ок", "Когда?"]),
    (["голодный", "пообедаем", "поужинаем"], ["Давай", "Го", "Ок"]),
    (["поможешь", "можешь помочь", "нужна помощь"], ["Конечно", "Сейчас", "Что случилось?"]),
    (["я про это хотела написать", "как раз хотел написать", "как раз думал"], ["О чём?", "Расскажи", "Интересно"]),
    (["отлично", "супер", "круто", "класс"], ["👍", "Ага", "Рад"]),
    (["пока", "до связи"], ["Пока", "Давай", "До связи"]),
]


def norm_reply(r: str) -> str:
    return re.sub(r"\s+", " ", (r or "").strip().lower())


def tokens(text: str) -> set[str]:
    out = set()
    for raw in re.split(r"[^\w]+", text.lower(), flags=re.UNICODE):
        if len(raw) <= 1 or raw in STOP:
            continue
        out.add(raw)
    return out


def is_clean_pair(context: str, reply: str) -> bool:
    c = (context or "").strip()
    r = (reply or "").strip()
    if not c or not r:
        return False
    if len(c) < 2 or len(c) > 72:
        return False
    if len(r) < 1 or len(r) > 36:
        return False
    if re.search(r"https?://|www\.|t\.me/", c, re.I):
        return False
    if re.search(r"\d{4,}", c) and len(tokens(c)) > 6:
        return False
    # catalog / product noise
    if len(tokens(c)) > 10:
        return False
    nr = norm_reply(r)
    if nr in CATCH_ALL_REPLIES:
        return False
    if re.fullmatch(r"[\W\d_]+", nr):
        return False
    # too personal pet-talk dumps
    if any(x in c.lower() for x in ("котёноч", "котеноч", "солнышк", "пастилу", "мяумяу")):
        return False
    return bool(tokens(c))


def merge_intents(base: list[dict]) -> list[dict]:
    by_id = {i["id"]: dict(i) for i in base}
    for extra in EXTRA_INTENTS:
        if extra["id"] in by_id:
            cur = by_id[extra["id"]]
            pats = list(dict.fromkeys([*cur.get("patterns", []), *extra["patterns"]]))
            chips = list(dict.fromkeys([*cur.get("chips", []), *extra["chips"]]))
            cur["patterns"] = pats
            cur["chips"] = chips
        else:
            by_id[extra["id"]] = dict(extra)
    # prefer learning bank if available
    if LEARNING_INTENT.exists():
        learn = json.loads(LEARNING_INTENT.read_text(encoding="utf-8"))
        for i in learn.get("intents", []):
            iid = i.get("id")
            if not iid:
                continue
            if iid not in by_id:
                by_id[iid] = i
            else:
                cur = by_id[iid]
                cur["patterns"] = list(dict.fromkeys([*cur.get("patterns", []), *i.get("patterns", [])]))
                cur["chips"] = list(dict.fromkeys([*cur.get("chips", []), *i.get("chips", [])]))
    return list(by_id.values())


def build_curated() -> list[dict]:
    pairs = []
    seen = set()
    for contexts, replies in CURATED_TEMPLATES:
        for ctx in contexts:
            for reply in replies:
                key = (norm_reply(ctx), norm_reply(reply))
                if key in seen:
                    continue
                seen.add(key)
                pairs.append({"context": ctx, "reply": reply, "source": "curated"})
    return pairs


def clean_legacy(raw_pairs: list[dict], reply_freq: Counter) -> list[dict]:
    out = []
    seen = set()
    for p in raw_pairs:
        ctx = p.get("context", "")
        reply = p.get("reply", "")
        if not is_clean_pair(ctx, reply):
            continue
        nr = norm_reply(reply)
        # drop replies that were massively overused in the dump
        if reply_freq.get(nr, 0) >= 40 and nr not in {"да", "нет", "ага", "угу", "ок", "окей", "хорошо", "хз"}:
            continue
        if reply_freq.get(nr, 0) >= 20 and nr in CATCH_ALL_REPLIES:
            continue
        key = (norm_reply(ctx), nr)
        if key in seen:
            continue
        seen.add(key)
        out.append({"context": ctx.strip(), "reply": reply.strip(), "source": "legacy"})
    return out


def main() -> None:
    src = json.loads(SRC.read_text(encoding="utf-8"))
    intents = merge_intents(src.get("intents", []))
    raw_pairs = src.get("pairs", [])
    reply_freq = Counter(norm_reply(p.get("reply", "")) for p in raw_pairs)

    curated = build_curated()
    legacy = clean_legacy(raw_pairs, reply_freq)

    # curated wins on duplicate context+reply
    seen = {(norm_reply(p["context"]), norm_reply(p["reply"])) for p in curated}
    merged = list(curated)
    for p in legacy:
        key = (norm_reply(p["context"]), norm_reply(p["reply"]))
        if key in seen:
            continue
        seen.add(key)
        merged.append(p)

    out = {
        "version": 2,
        "name": "fluffy_smart_reply_ru",
        "lang": "ru",
        "retrieval": {
            "method": "hybrid_weighted",
            "word_weight": 0.68,
            "char_weight": 0.32,
            "source_weights": {"curated": 1.0, "legacy": 0.55, "external": 0.65},
            "top_k": 3,
        },
        "stats": {
            "n_pairs": len(merged),
            "n_curated": sum(1 for p in merged if p["source"] == "curated"),
            "n_legacy": sum(1 for p in merged if p["source"] == "legacy"),
            "n_unique_replies": len({norm_reply(p["reply"]) for p in merged}),
            "n_intents": len(intents),
        },
        "intents": intents,
        "pairs": merged,
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(json.dumps(out["stats"], ensure_ascii=False, indent=2))
    print("wrote", OUT, "size", OUT.stat().st_size)


if __name__ == "__main__":
    main()
