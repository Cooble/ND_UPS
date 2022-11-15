#pragma once
#include "ndpch.h"
#include "net_iterator.h"
#include "net/net.h"


struct NetReader;
struct NetWriter;

template <typename Self>
struct Serializable
{
	~Serializable()
	{
		static_assert(sizeof(decltype(&Self::deserialize)) + sizeof(decltype(&Self::serialize)),
			"Missing methods serialize");
	}

	//using CheckFunExist = std::void_t<decltype(&Self::deserialize), decltype(&Self::serialize)>;

	using SelfType = Self;

	/*bool deserialize(NetReader& reader)
	{
		return false;
	}

	void serialize(NetWriter& reader) const
	{
	}*/
};

namespace NetConversions
{
	template <typename T>
	void serialize(const T& from, NetWriter& r) = delete;
	template <typename T>
	bool deserialize(T& to, NetReader& r) = delete;

	template <typename T>
	bool deserializeArray(std::vector<T>& to, NetReader& r);
	template <typename T>
	void serializeArray(const std::vector<T>& from, NetWriter& r);


	template <typename T, class = void>
	struct is_convertible : std::false_type
	{
	};

	template <typename T>
	struct is_convertible<T, decltype(serialize<T>(std::declval<const T&>(), std::declval<NetWriter&>()), void()
	                      )> : std::true_type
	{
	};

	template <class T>
	struct is_array
	{
		static constexpr bool value = false;
	};

	template <class T>
	struct is_array<std::vector<T>>
	{
		static constexpr bool value = true;
	};
}

struct NetReader : nd::net::BufferReader
{
	NetReader(nd::net::Buffer& b): BufferReader(b)
	{
	}

	template <typename T>
	bool get(T& t)
	{
		if constexpr (std::is_base_of_v<Serializable<T>, T>)
			return t.deserialize(*this);
		if constexpr (NetConversions::is_convertible<T>::value)
		{
			return NetConversions::deserialize(t, *this);
		}
		if constexpr (NetConversions::is_array<T>::value)
		{
			return NetConversions::deserializeArray(t, *this);
		}

		auto out = BufferReader::tryRead<T>();
		if (!out)
			return false;
		t = *out;
		return true;
	}

	bool get(std::string& s, int maxLength)
	{
		s = readStringUntilNull(maxLength);
		return !s.empty();
	}
};

struct NetWriter : nd::net::BufferWriter
{
	NetWriter(nd::net::Buffer& b, bool append = false, bool expand = true) : BufferWriter(b, append, expand)
	{
	}

	template <typename T>
	void put(const T& t)
	{
		if constexpr (std::is_base_of_v<Serializable<T>, T>)
		{
			t.serialize(*this);
			return;
		}
		if constexpr (NetConversions::is_convertible<T>::value)
		{
			NetConversions::serialize(t, *this);
			return;
		}
		if constexpr (NetConversions::is_array<T>::value)
		{
			NetConversions::serializeArray(t, *this);
			return;
		}

		BufferWriter::write<T>(t);
	}
};

inline NetWriter NetAppender(nd::net::Buffer& b) { return {b, true, true}; }

namespace NetConversions
{
	template <>
	inline void serialize<uint32_t>(const uint32_t& from, NetWriter& r)
	{
		r.write(std::to_string(from));
	}

	template <>
	inline void serialize<int>(const int& from, NetWriter& r)
	{
		r.write(std::to_string(from));
	}

	template <>
	inline void serialize<float>(const float& from, NetWriter& r)
	{
		r.write(std::to_string(from));
	}

	template <>
	inline void serialize<double>(const double& from, NetWriter& r)
	{
		r.write(std::to_string(from));
	}

	template <>
	inline void serialize<std::string>(const std::string& from, NetWriter& r)
	{
		r.write(from);
	}

	template <>
	inline void serialize<glm::vec2>(const glm::vec2& from, NetWriter& r)
	{
		r.put(from.x);
		r.put(from.y);
	}

	template <typename T>
	void serializeArray(const std::vector<T>& from, NetWriter& r)
	{
		r.put(from.size());

		for (auto& t : from)
			r.put(t);
	}

	// ===============================================

	template <>
	inline bool deserialize<uint32_t>(uint32_t& to, NetReader& r)
	{
		auto s = r.readStringUntilNull(11);
		auto c = const_cast<char*>(s.c_str());
		errno = 0;
		auto out = strtoul(s.c_str(), &c, 10);
		if (errno)
			return false;

		to = out;
		return true;
	}

	template <>
	inline bool deserialize<int>(int& to, NetReader& r)
	{
		auto s = r.readStringUntilNull(11);
		auto c = const_cast<char*>(s.c_str());
		errno = 0;
		auto out = strtol(s.c_str(), &c, 10);
		if (errno)
			return false;

		to = out;
		return true;
	}

	template <>
	inline bool deserialize<float>(float& to, NetReader& r)
	{
		auto s = r.readStringUntilNull(-1);
		auto c = const_cast<char*>(s.c_str());
		errno = 0;
		auto out = strtof(s.c_str(), &c);
		if (errno)
			return false;

		to = out;
		return true;
	}

	template <>
	inline bool deserialize<double>(double& to, NetReader& r)
	{
		auto s = r.readStringUntilNull(-1);
		auto c = const_cast<char*>(s.c_str());
		errno = 0;
		auto out = strtod(s.c_str(), &c);
		if (errno)
			return false;

		to = out;
		return true;
	}

	template <>
	inline bool deserialize<std::string>(std::string& to, NetReader& r)
	{
		to = r.readStringUntilNull(-1);
		return !to.empty();
	}

	template <>
	inline bool deserialize<glm::vec2>(glm::vec2& to, NetReader& r)
	{
		return r.get(to.x) && r.get(to.y);
	}

	template <typename T>
	bool deserializeArray(std::vector<T>& to, NetReader& r)
	{
		int size;
		if (!r.get(size))
			return false;
		to.reserve(size);
		to.resize(size);
		for (int i = 0; i < size; ++i)
			if (!r.get(to[i]))
				return false;

		return true;
	}
}
