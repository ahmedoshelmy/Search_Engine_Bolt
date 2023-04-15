import React, { useState } from "react";
import useSound from "use-sound";
import { BsFillKeyboardFill } from "react-icons/bs";
import bolt from "../assets/Vector.png";
import keyPressSound from "../assets/keyPress.mp3";
import Voice from "./Voice";
export default function Search() {
	const [play] = useSound(keyPressSound, { volume: 0.7 });
	const [txt, setText] = useState("");

	const handelInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
		setText(e.target.value);
		play();
	};
	return (
		<div className="flex flex-row justify-between items-center py-1 px-6  w-7/12 border-2 rounded-full border-highlight bg-overlay text-highlight">
			<img src={bolt} alt="bolt" width={15} />
			<input
				type="text"
				className=" flex-grow ml-2 border-0 outline-none bg-transparent"
				onChange={handelInputChange}
				value={txt}
			/>
			<div className="flex flex-row gap-1 text-2xl ">
				<Voice txt={txt} setText={setText} />
				<KeyBoard />
			</div>
		</div>
	);
}

const KeyBoard = () => {
	return (
		<div className="hover:bg-primary cursor-pointer rounded-full px-2 py-2">
			<BsFillKeyboardFill />
		</div>
	);
};